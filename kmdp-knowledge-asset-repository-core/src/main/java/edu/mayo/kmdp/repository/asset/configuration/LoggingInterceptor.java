package edu.mayo.kmdp.repository.asset.configuration;

import edu.mayo.kmdp.util.ws.ExternalBundleMessageProvider;
import java.util.Locale;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.omg.spec.api4kp._20200801.aspects.LoggingAdvisingInterceptor;

@Aspect
public class LoggingInterceptor extends LoggingAdvisingInterceptor {

  static ExternalBundleMessageProvider provider =
      new ExternalBundleMessageProvider("messages/knowledgeArtifactRepositoryMessages");

  @Pointcut("within(edu.mayo.kmdp.repository.asset.*)")
  public void getScope() {
    // pointcut
  }

  @Override
  @Around("getLog() && getScope()")
  public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    return super.logExecution(joinPoint);
  }

  @Override
  protected String getMessage(String msgCode, Object[] args, String defaultMessage,
      Locale aDefault) {
    return codifyMessage(msgCode, provider.getMessage(msgCode, args, defaultMessage, aDefault));
  }

}
