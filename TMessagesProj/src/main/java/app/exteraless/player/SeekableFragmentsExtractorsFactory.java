package app.exteraless.player;

import android.net.Uri;

import androidx.media3.common.C;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.Extractor;
import androidx.media3.extractor.ExtractorInput;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.ForwardingExtractor;
import androidx.media3.extractor.ForwardingExtractorOutput;
import androidx.media3.extractor.PositionHolder;
import androidx.media3.extractor.SeekMap;
import androidx.media3.extractor.SeekPoint;
import androidx.media3.extractor.TrackOutput;
import androidx.media3.extractor.mp4.FragmentedMp4Extractor;
import androidx.media3.extractor.text.SubtitleParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class SeekableFragmentsExtractorsFactory implements ExtractorsFactory {

    private static final int RESYNC_WINDOW = 64 * 1024;
    private static final int SIGNATURE_LENGTH = 16;
    private static final long MAX_FRAGMENT_HEADER_SIZE = 16 * 1024 * 1024;

    private final DefaultExtractorsFactory delegate = new DefaultExtractorsFactory();

    @Override
    public Extractor[] createExtractors() {
        return wrap(delegate.createExtractors());
    }

    @Override
    public Extractor[] createExtractors(Uri uri, Map<String, List<String>> responseHeaders) {
        return wrap(delegate.createExtractors(uri, responseHeaders));
    }

    @Override
    public ExtractorsFactory experimentalSetTextTrackTranscodingEnabled(boolean textTrackTranscodingEnabled) {
        delegate.experimentalSetTextTrackTranscodingEnabled(textTrackTranscodingEnabled);
        return this;
    }

    @Override
    public ExtractorsFactory setSubtitleParserFactory(SubtitleParser.Factory subtitleParserFactory) {
        delegate.setSubtitleParserFactory(subtitleParserFactory);
        return this;
    }

    @Override
    public ExtractorsFactory experimentalSetCodecsToParseWithinGopSampleDependencies(int codecsToParseWithinGopSampleDependencies) {
        delegate.experimentalSetCodecsToParseWithinGopSampleDependencies(codecsToParseWithinGopSampleDependencies);
        return this;
    }

    private static Extractor[] wrap(Extractor[] extractors) {
        for (int i = 0; i < extractors.length; i++) {
            if (extractors[i].getUnderlyingImplementation() instanceof FragmentedMp4Extractor) {
                extractors[i] = new SeekableFragmentsExtractor(extractors[i]);
            }
        }
        return extractors;
    }

    static final class SeekableFragmentsExtractor extends ForwardingExtractor {

        private long inputLength = C.LENGTH_UNSET;
        private long firstFragmentPosition = C.INDEX_UNSET;
        private boolean onlyAudio = true;
        private boolean anyTrack;
        private boolean pendingResync;

        SeekableFragmentsExtractor(Extractor delegate) {
            super(delegate);
        }

        @Override
        public void init(ExtractorOutput output) {
            super.init(new ForwardingExtractorOutput(output) {
                @Override
                public TrackOutput track(int id, int type) {
                    anyTrack = true;
                    if (type != C.TRACK_TYPE_AUDIO) {
                        onlyAudio = false;
                    }
                    return super.track(id, type);
                }

                @Override
                public void seekMap(SeekMap seekMap) {
                    super.seekMap(estimateIfUnseekable(seekMap));
                }
            });
        }

        @Override
        public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
            inputLength = input.getLength();
            if (pendingResync) {
                pendingResync = false;
                if (!skipToNextFragment(input)) {
                    return RESULT_END_OF_INPUT;
                }
            }
            return super.read(input, seekPosition);
        }

        @Override
        public void seek(long position, long timeUs) {
            super.seek(position, timeUs);
            pendingResync = firstFragmentPosition != C.INDEX_UNSET && position > firstFragmentPosition;
        }

        private SeekMap estimateIfUnseekable(SeekMap seekMap) {
            if (seekMap.isSeekable() || !anyTrack || !onlyAudio) {
                return seekMap;
            }
            final long durationUs = seekMap.getDurationUs();
            final long start = seekMap.getSeekPoints(0).first.position;
            final long length = inputLength;
            if (durationUs == C.TIME_UNSET || durationUs <= 0 || length == C.LENGTH_UNSET || length <= start) {
                return seekMap;
            }
            firstFragmentPosition = start;
            return new EstimateSeekMap(durationUs, start, length);
        }

        private static boolean skipToNextFragment(ExtractorInput input) throws IOException {
            final byte[] window = new byte[RESYNC_WINDOW];
            while (true) {
                int filled = 0;
                while (filled < window.length) {
                    int read = input.peek(window, filled, window.length - filled);
                    if (read == C.RESULT_END_OF_INPUT) {
                        break;
                    }
                    filled += read;
                }
                input.resetPeekPosition();
                for (int i = 0; i + SIGNATURE_LENGTH <= filled; i++) {
                    if (isFragmentStart(window, i)) {
                        input.skipFully(i);
                        return true;
                    }
                }
                if (filled < SIGNATURE_LENGTH) {
                    return false;
                }
                input.skipFully(filled - SIGNATURE_LENGTH + 1);
            }
        }

        private static boolean isFragmentStart(byte[] data, int offset) {
            if (data[offset + 4] != 'm' || data[offset + 5] != 'o' || data[offset + 6] != 'o'
                    || data[offset + 7] != 'f' || data[offset + 12] != 'm' || data[offset + 13] != 'f'
                    || data[offset + 14] != 'h' || data[offset + 15] != 'd') {
                return false;
            }
            long size = ((data[offset] & 0xFFL) << 24) | ((data[offset + 1] & 0xFFL) << 16)
                    | ((data[offset + 2] & 0xFFL) << 8) | (data[offset + 3] & 0xFFL);
            return size >= SIGNATURE_LENGTH && size <= MAX_FRAGMENT_HEADER_SIZE;
        }
    }

    static final class EstimateSeekMap implements SeekMap {

        private final long durationUs;
        private final long firstFragmentPosition;
        private final long inputLength;

        EstimateSeekMap(long durationUs, long firstFragmentPosition, long inputLength) {
            this.durationUs = durationUs;
            this.firstFragmentPosition = firstFragmentPosition;
            this.inputLength = inputLength;
        }

        @Override
        public boolean isSeekable() {
            return true;
        }

        @Override
        public long getDurationUs() {
            return durationUs;
        }

        @Override
        public SeekPoints getSeekPoints(long timeUs) {
            final long span = inputLength - firstFragmentPosition;
            final long clamped = Math.max(0, Math.min(timeUs, durationUs));
            final long position = firstFragmentPosition + (long) ((double) span * clamped / durationUs) - span / 20;
            if (position <= firstFragmentPosition) {
                return new SeekPoints(new SeekPoint(0, firstFragmentPosition));
            }
            return new SeekPoints(new SeekPoint(clamped, Math.min(position, inputLength - 1)));
        }
    }
}
