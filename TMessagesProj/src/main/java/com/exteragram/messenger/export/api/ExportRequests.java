package com.exteragram.messenger.export.api;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public final class ExportRequests {

    private ExportRequests() {
    }

    public static class InvokeWithMessagesRange extends TLObject {
        public TLObject query;
        public TLRPC.TL_messageRange range;
    }

    public static class InvokeWithTakeoutWrapper extends TLObject {
        public TLObject query;
        public long takeout_id;
    }
}
