package com.cursosdedesarrollo.websockettwitchchat.services;

import com.cursosdedesarrollo.websockettwitchchat.domain.ChannelAuthorization;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.condition.ChannelEventSubCondition;
import com.github.twitch4j.eventsub.condition.ChannelFollowV2Condition;
import com.github.twitch4j.eventsub.condition.ChannelRaidCondition;
import com.github.twitch4j.eventsub.socket.IEventSubSocket;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

class TwitchEventSubServiceTest {
    @Test
    void registersAllRequiredSubscriptionsForOnlyTheAuthorizedChannel() {
        IEventSubSocket socket = mock(IEventSubSocket.class);
        ChannelAuthorization authorization = new ChannelAuthorization(
                "twitch", "channel-123", "streamer", "synthetic-access-token", "synthetic-refresh-token",
                List.of("chat:read", "chat:edit"), Instant.parse("2026-02-01T00:00:00Z"),
                ChannelAuthorization.Status.AUTHORIZED, Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));

        new TwitchEventSubService().registerChannelSubscriptions(socket, authorization);

        ArgumentCaptor<EventSubSubscription> subscriptions = ArgumentCaptor.forClass(EventSubSubscription.class);
        verify(socket, times(7)).register(any(OAuth2Credential.class), subscriptions.capture());
        Map<String, List<EventSubSubscription>> byType = subscriptions.getAllValues().stream()
                .collect(Collectors.groupingBy(EventSubSubscription::getRawType));

        assertEquals(Set.of("channel.follow", "channel.cheer", "channel.raid", "channel.subscribe",
                "channel.subscription.gift", "channel.subscription.message"), byType.keySet());
        assertEquals(2, byType.get("channel.raid").size());
        for (String type : List.of("channel.cheer", "channel.subscribe", "channel.subscription.gift",
                "channel.subscription.message")) {
            ChannelEventSubCondition condition = (ChannelEventSubCondition) byType.get(type).getFirst().getCondition();
            assertEquals("channel-123", condition.getBroadcasterUserId());
        }
        ChannelFollowV2Condition follow = (ChannelFollowV2Condition) byType.get("channel.follow").getFirst().getCondition();
        assertEquals("channel-123", follow.getBroadcasterUserId());
        assertEquals("channel-123", follow.getModeratorUserId());

        Set<String> raidDirections = byType.get("channel.raid").stream()
                .map(subscription -> (ChannelRaidCondition) subscription.getCondition())
                .map(condition -> condition.getFromBroadcasterUserId() == null ? "incoming" : "outgoing")
                .collect(Collectors.toSet());
        assertEquals(Set.of("incoming", "outgoing"), raidDirections);
    }

    @Test
    void rejectsAuthorizationFromAnotherProvider() {
        IEventSubSocket socket = mock(IEventSubSocket.class);
        ChannelAuthorization authorization = new ChannelAuthorization(
                "youtube", "channel-123", "streamer", "access", "refresh", List.of(),
                Instant.parse("2026-02-01T00:00:00Z"), ChannelAuthorization.Status.AUTHORIZED,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new TwitchEventSubService().registerChannelSubscriptions(socket, authorization));
        org.mockito.Mockito.verifyNoInteractions(socket);
    }
}
