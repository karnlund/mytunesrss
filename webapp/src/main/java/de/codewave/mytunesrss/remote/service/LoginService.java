/*
 * LoginService.java 30.01.2008
 *
 * Copyright (c) 2008 WEB.DE GmbH, Karlsruhe. All rights reserved.
 *
 * $Id$
 */
package de.codewave.mytunesrss.remote.service;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.User;
import de.codewave.mytunesrss.remote.MyTunesRssRemoteEnv;
import de.codewave.mytunesrss.remote.Session;
import de.codewave.mytunesrss.remote.render.RenderMachine;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class LoginService {
    public String login(String username, String password, int sessionTimeoutMinutes) throws UnsupportedEncodingException, IllegalAccessException {
        User user = MyTunesRss.CONFIG.getUser(username);
        if (user != null) {
            byte[] passwordHash = MyTunesRss.SHA1_DIGEST.digest(password.getBytes("UTF-8"));
            if (Arrays.equals(user.getPasswordHash(), passwordHash) && user.isActive()) {
                MyTunesRssRemoteEnv.getRequest().getSession().setAttribute("remoteApiUser", user);
                String sid = new String(Hex.encodeHex(MyTunesRss.MD5_DIGEST.digest((MyTunesRssRemoteEnv.getRequest().getSession().getId() +
                        System.currentTimeMillis()).getBytes("UTF-8"))));
                MyTunesRssRemoteEnv.addSession(new Session(sid, user, sessionTimeoutMinutes * 60000));
                return sid;
            }
        }
        throw new IllegalAccessException("Unauthorized");
    }

    public void logout() throws IllegalAccessException {
        User user = MyTunesRssRemoteEnv.getSession().getUser();
        if (user != null) {
            MyTunesRssRemoteEnv.getSession().invalidate();
        } else {
            throw new IllegalAccessException("Unauthorized");
        }
    }

    public boolean ping() {
        return MyTunesRssRemoteEnv.getSession() != null && MyTunesRssRemoteEnv.getSession().getUser() != null;
    }

    public Object getUserInfo() throws IllegalAccessException {
        User user = MyTunesRssRemoteEnv.getSession().getUser();
        if (user != null) {
            return RenderMachine.getInstance().render(user);
        } else {
            throw new IllegalAccessException("Unauthorized");
        }
    }

    public void saveUserSettings(String password, String email, String lastFmUser, String lastFmPassword) throws IllegalAccessException,
            UnsupportedEncodingException {
        User user = MyTunesRssRemoteEnv.getSession().getUser();
        if (user != null) {
            if (user.isChangeEmail() && StringUtils.isNotBlank(email)) {
                user.setEmail(StringUtils.trim(email));
            }
            if (user.isChangePassword() && StringUtils.isNotBlank(password)) {
                user.setPasswordHash(MyTunesRss.SHA1_DIGEST.digest(StringUtils.trim(password).getBytes("UTF-8")));
            }
            if (user.isEditLastFmAccount()) {
                if (StringUtils.isNotBlank(lastFmUser)) {
                    user.setLastFmUsername(StringUtils.trim(lastFmUser));
                }
                if (StringUtils.isNotBlank(lastFmPassword)) {
                    user.setLastFmPasswordHash(MyTunesRss.MD5_DIGEST.digest(StringUtils.trim(lastFmPassword).getBytes("UTF-8")));
                }
            }
        } else {
            throw new IllegalAccessException("Unauthorized");
        }
    }
}
