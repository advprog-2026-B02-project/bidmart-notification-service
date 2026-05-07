package id.ac.ui.cs.advprog.bidmart.notifications.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Test
    void configureMessageBroker_shouldEnableTopicAndQueueAndPrefixes() {
        WebSocketConfig config = new WebSocketConfig(mock(WebSocketAuthInterceptor.class));
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic", "/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
        verify(registry).setUserDestinationPrefix("/user");
    }

    @Test
    void registerStompEndpoints_shouldRegisterNotificationsEndpointWithSockJs() {
        WebSocketConfig config = new WebSocketConfig(mock(WebSocketAuthInterceptor.class));
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJsRegistration = mock(SockJsServiceRegistration.class);

        when(registry.addEndpoint("/ws/notifications")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("*")).thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsRegistration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws/notifications");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).withSockJS();
    }

    @Test
    void configureClientInboundChannel_shouldRegisterAuthInterceptor() {
        WebSocketAuthInterceptor interceptor = mock(WebSocketAuthInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(interceptor);
        ChannelRegistration registration = mock(ChannelRegistration.class);

        config.configureClientInboundChannel(registration);

        verify(registration).interceptors(interceptor);
    }
}