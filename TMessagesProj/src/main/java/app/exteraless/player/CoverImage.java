package app.exteraless.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.Components.BackupImageView;

public class CoverImage extends BackupImageView {

    public interface Listener {
        void onSeed(MessageObject messageObject, int seed);
    }

    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final PlayerIcon note;
    private int radius;
    private String currentKey;
    private MessageObject message;
    private int fallbackSeed;
    private Listener listener;

    public CoverImage(Context context, int noteSizeDp) {
        super(context);
        note = PlayerIcon.stroke(PlayerIcon.NOTE, noteSizeDp);
        getImageReceiver().setDelegate((imageReceiver, set, thumb, memCache) -> {
            if (set) {
                onBitmap(imageReceiver.getBitmap());
            }
        });
    }

    public void setListener(int fallbackSeed, Listener listener) {
        this.fallbackSeed = fallbackSeed;
        this.listener = listener;
    }

    public void setRadius(int r) {
        radius = r;
        setRoundRadius(r);
        invalidate();
    }

    public void setColors(int background, int foreground) {
        placeholderPaint.setColor(background);
        note.setColor(foreground);
        invalidate();
    }

    public MessageObject getMessage() {
        return message;
    }

    public void setMessage(MessageObject messageObject) {
        if (messageObject == null) {
            currentKey = null;
            message = null;
            setImageDrawable(null);
            return;
        }
        Bitmap file = PlayerArt.fileCover(messageObject);
        String key = PlayerArt.key(messageObject) + (file != null ? ":f" : "");
        if (key.equals(currentKey)) {
            return;
        }
        currentKey = key;
        message = messageObject;
        if (file != null) {
            setImageBitmap(file);
            onBitmap(file);
            return;
        }
        ImageLocation full = PlayerArt.fullLocation(messageObject);
        ImageLocation thumb = PlayerArt.thumbLocation(messageObject);
        if (full != null) {
            setImage(full, null, thumb, null, null, 0, 1, messageObject);
        } else if (thumb != null) {
            setImage(null, null, thumb, null, null, 0, 1, messageObject);
        } else {
            setImageDrawable(null);
        }
    }

    private void onBitmap(Bitmap bitmap) {
        final MessageObject target = message;
        if (bitmap == null || target == null || listener == null) {
            return;
        }
        PlayerArt.requestSeed(target, bitmap, fallbackSeed, seed -> {
            if (target == message && listener != null) {
                listener.onSeed(target, seed);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!getImageReceiver().hasBitmapImage()) {
            rect.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(rect, radius, radius, placeholderPaint);
            note.setBounds(0, 0, getWidth(), getHeight());
            note.draw(canvas);
        }
        super.onDraw(canvas);
    }
}
