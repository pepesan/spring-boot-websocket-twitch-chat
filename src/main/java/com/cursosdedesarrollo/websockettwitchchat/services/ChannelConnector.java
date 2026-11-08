package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;

public interface ChannelConnector {
    String providerId();

    void connect(ChannelAuthorization authorization);
}
