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
        StompWebSocketEndpointRegistration prefixedEndpointRegistration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJsRegistration = mock(SockJsServiceRegistration.class);
        SockJsServiceRegistration prefixedSockJsRegistration = mock(SockJsServiceRegistration.class);

        when(registry.addEndpoint("/ws/notifications")).thenReturn(endpointRegistration);
        when(registry.addEndpoint("/api/notifications/ws/notifications")).thenReturn(prefixedEndpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("*")).thenReturn(endpointRegistration);
        when(prefixedEndpointRegistration.setAllowedOriginPatterns("*")).thenReturn(prefixedEndpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsRegistration);
        when(prefixedEndpointRegistration.withSockJS()).thenReturn(prefixedSockJsRegistration);

        config.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws/notifications");
        verify(registry).addEndpoint("/api/notifications/ws/notifications");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(prefixedEndpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).withSockJS();
        verify(prefixedEndpointRegistration).withSockJS();
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