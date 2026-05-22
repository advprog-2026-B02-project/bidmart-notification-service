package id.ac.ui.cs.advprog.bidmart.notifications.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class WebSocketAuthInterceptorTest {

    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor();

    @Test
    void preSend_shouldSetUserFromConnectHeader() {
        String userId = java.util.UUID.randomUUID().toString();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("X-User-Id", userId);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertEquals(userId, resultAccessor.getUser().getName());
    }

    @Test
    void preSend_shouldLeaveMessageUntouchedWhenCommandIsNotConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    void preSend_shouldReturnPlainMessageWhenNoStompAccessorExists() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertSame(message, result);
    }
}