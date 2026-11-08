package com.cursosdedesarrollo.websockettwitchchat.channels;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;

public interface ChannelConnector {
    String providerId();

    void connect(ChannelAuthorization authorization);

    void disconnect(ChannelAuthorization authorization);
    boolean isConnected(ChannelAuthorization authorization);
}
