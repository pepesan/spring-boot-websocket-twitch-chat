package com.cursosdedesarrollo.websockettwitchchat.twitch.events;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.eventsub.socket.IEventSubSocket;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import org.springframework.stereotype.Service;

@Service
public class TwitchEventSubService {
    public void registerChannelSubscriptions(IEventSubSocket socket, ChannelAuthorization authorization) {
        if (!"twitch".equals(authorization.providerId())) {
            throw new IllegalArgumentException("Authorization belongs to a different EventSub provider");
        }
        String broadcasterId = authorization.providerUserId();
        OAuth2Credential credential = new OAuth2Credential("twitch", authorization.accessToken());

        socket.register(credential, SubscriptionTypes.CHANNEL_FOLLOW_V2.prepareSubscription(
                condition -> condition.broadcasterUserId(broadcasterId).moderatorUserId(broadcasterId).build(), null));
        socket.register(credential, SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(
                condition -> condition.broadcasterUserId(broadcasterId).build(), null));
        socket.register(credential, SubscriptionTypes.CHANNEL_RAID.prepareSubscription(
                condition -> condition.toBroadcasterUserId(broadcasterId).build(), null));
        socket.register(credential, SubscriptionTypes.CHANNEL_RAID.prepareSubscription(
                condition -> condition.fromBroadcasterUserId(broadcasterId).build(), null));
        socket.register(credential, SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(
                condition -> condition.broadcasterUserId(broadcasterId).build(), null));
        socket.register(credential, SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT.prepareSubscription(
                condition -> condition.broadcasterUserId(broadcasterId).build(), null));
        socket.register(credential, SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE.prepareSubscription(
                condition -> condition.broadcasterUserId(broadcasterId).build(), null));
    }
}
