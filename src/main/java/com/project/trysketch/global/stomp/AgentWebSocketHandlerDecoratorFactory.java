package com.project.trysketch.global.stomp;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

public class AgentWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {
    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTargetClass(AopUtils.getTargetClass(handler));
        proxyFactory.setTargetSource(new SingletonTargetSource(handler));
        proxyFactory.addAdvisor(new DefaultIntroductionAdvisor(new SubProtocolWebSocketHandlerInterceptor()));
        proxyFactory.setOptimize(true);
        proxyFactory.setExposeProxy(true);
        return (WebSocketHandler) proxyFactory.getProxy();
    }
}
