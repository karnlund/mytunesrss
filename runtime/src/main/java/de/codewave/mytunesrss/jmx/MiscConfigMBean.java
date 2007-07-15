package de.codewave.mytunesrss.jmx;

import java.io.*;

/**
 * <b>Description:</b>   <br>
 * <b>Copyright:</b>     Copyright (c) 2007<br>
 * <b>Company:</b>       Cologne Systems GmbH<br>
 * <b>Creation Date:</b> 01.03.2007
 *
 * @author Michael Descher
 * @version 1.0
 */
public interface MiscConfigMBean {
  boolean isUseProxy();
  void setUseProxy(boolean useProxy);
  String getProxyHost();
  void setProxyHost(String proxyHost);
  int getProxyPort();
  void setProxyPort(int port);
  String getMyTunesRssComUser();
  void setMyTunesRssComUser(String usermame);
  void setMyTunesRssComPassword(String password) throws UnsupportedEncodingException;
}



