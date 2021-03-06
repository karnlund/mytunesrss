<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/tags" prefix="mt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>

<%--@elvariable id="appUrl" type="java.lang.String"--%>
<%--@elvariable id="servletUrl" type="java.lang.String"--%>
<%--@elvariable id="permFeedServletUrl" type="java.lang.String"--%>
<%--@elvariable id="auth" type="java.lang.String"--%>
<%--@elvariable id="authUser" type="de.codewave.mytunesrss.config.User"--%>
<%--@elvariable id="globalConfig" type="de.codewave.mytunesrss.config.MyTunesRssConfig"--%>
<%--@elvariable id="config" type="de.codewave.mytunesrss.servlet.WebConfig"--%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <jsp:include page="incl_head.jsp"/>

    <script type="text/javascript">
        function toggleDownload() {
            if (document.getElementById("downloadCheckbox").checked) {
                document.getElementById("yahooPlayerCheckbox").disabled = false;
            } else {
                document.getElementById("yahooPlayerCheckbox").disabled = true;
            }
        }
    </script>
</head>

<body class="settings">

    <div class="body">

        <div class="head">
            <h1>
                <a class="portal" href="${servletUrl}/showPortal/${auth}"><span id="linkPortal"><fmt:message key="portal" /></span></a>
                <span><fmt:message key="myTunesRss" /></span>
            </h1>
        </div>

        <div class="content">

            <div class="content-inner">

                <jsp:include page="/incl_error.jsp" />

                <form action="${servletUrl}/saveSettings/${auth}" method="post" onsubmit="document.getElementById('yahooPlayerCheckbox').disabled=false;return true">
                    <table cellspacing="0" class="settings">
                        <tr>
                            <th colspan="2"><fmt:message key="settings.info" /></th>
                        </tr>
                        <mt:initFlipFlop value1="" value2="class=\"odd\""/>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.username" /></td>
                            <td><c:out value="${authUser.name}"/></td>
                        </tr>
                        <c:if test="${!authUser.editLastFmAccount && !empty authUser.lastFmUsername}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.lastFmUsername" /></td>
                                <td><c:out value="${authUser.lastFmUsername}"/></td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.quota}">
                            <tr <mt:flipFlop/>>
                                <td><fmt:message key="settings.quota" /></td>
                                <td><c:out value="${mtfn:memory(authUser.bytesQuota)}"/> ${authUser.quotaType}</td>
                            </tr>
                            <tr <mt:flipFlop/>>
                                <td><fmt:message key="settings.quotaRemain" /></td>
                                <td><c:out value="${mtfn:memory(authUser.quotaRemaining)}"/></td>
                            </tr>
                        </c:if>
                        <tr>
                            <th colspan="2"><fmt:message key="settings" /></th>
                        </tr>
                        <mt:initFlipFlop value1="" value2="class=\"odd\""/>
                        <c:if test="${authUser.changePassword && !authUser.emptyPassword}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.password" /></td>
                                <td>
                                    <input type="password"
                                           name="password1"
                                           maxlength="30"
                                           value="<c:out value="${param.password1}"/>" />
                                </td>
                            </tr>
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.retypePassword" /></td>
                                <td>
                                    <input type="password"
                                           name="password2"
                                           maxlength="30"
                                           value="<c:out value="${param.password2}"/>" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.changeEmail}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.email" /></td>
                                <td>
                                    <input type="text"
                                           name="email"
                                           maxlength="30"
                                           value="<c:out value="${authUser.email}"/>" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.editLastFmAccount}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.lastFmUsername" /></td>
                                <td>
                                    <input type="text"
                                           name="lastfmusername"
                                           maxlength="30"
                                           value="<c:out value="${authUser.lastFmUsername}"/>" />
                                </td>
                            </tr>
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.lastFmPassword" /></td>
                                <td>
                                    <input type="password"
                                           name="lastfmpassword1"
                                           maxlength="30"
                                           value="<c:out value="${param.lastfmpassword1}"/>" />
                                </td>
                            </tr>
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.retypeLastFmPassword" /></td>
                                <td>
                                    <input type="password"
                                           name="lastfmpassword2"
                                           maxlength="30"
                                           value="<c:out value="${param.lastfmpassword2}"/>" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${!empty themes}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.theme" /></td>
                                <td>
                                    <select name="theme">
                                        <option value="" <c:if test="${empty config.theme}">selected="selected"</c:if>><fmt:message key="theme.default"/></option>
                                        <c:forEach items="${themes}" var="theme">
                                            <option value="<c:out value="${theme}"/>" <c:if test="${config.theme eq theme}">selected="selected"</c:if>><c:out value="${theme}"/></option>
                                        </c:forEach>
                                    </select>
                                </td>
                            </tr>
                        </c:if>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.keepAlive" /></td>
                            <td>
                                <input type="checkbox"
                                       name="keepAlive"
                                       value="true" <c:if test="${config.keepAlive}">checked="checked"</c:if>/>
                            </td>
                        </tr>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.itemsPerPage" /></td>
                            <td><input class="number"
                                       type="text"
                                       name="pageSize"
                                       maxlength="3"
                                       value="<c:out value="${cwfn:choose(config.pageSize > 0, config.pageSize, '')}"/>" /></td>
                        </tr>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.photoPageSize" /></td>
                            <td><input class="number"
                                       type="text"
                                       name="photoPageSize"
                                       maxlength="3"
                                       value="<c:out value="${cwfn:choose(config.photoPageSize > 0, config.photoPageSize, '')}"/>" /></td>
                        </tr>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.photoJpegQuality" /></td>
                            <td><input class="number"
                                       type="text"
                                       name="photoJpegQuality"
                                       maxlength="3"
                                       value="<c:out value="${config.photoJpegQuality}"/>" /></td>
                        </tr>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.maxSearchResults" /></td>
                            <td><input class="number"
                                       type="text"
                                       name="maxSearchResults"
                                       maxlength="5"
                                       value="<c:out value="${cwfn:choose(config.maxSearchResults > 0, config.maxSearchResults, '')}"/>" /></td>
                        </tr>
                        <c:if test="${!globalConfig.disableBrowser}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.browserStartIndex" /></td>
                                <td>
                                    <select name="browserStartIndex">
                                        <option value="" <c:if test="${config.browserStartIndex == ''}">selected="selected"</c:if>><fmt:message key="alphabetPagerAll"/></option>
                                        <option value="0" <c:if test="${config.browserStartIndex == '0'}">selected="selected"</c:if>>0 - 9</option>
                                        <option value="1" <c:if test="${config.browserStartIndex == '1'}">selected="selected"</c:if>>A - C</option>
                                    </select>
                                </td>
                            </tr>
                        </c:if>
                        <tr <mt:flipFlop/>>
                            <td class="label"><fmt:message key="settings.albumImageSize" /></td>
                            <td>
                                <select name="albImgSize">
                                    <option value="64" <c:if test="${config.albumImageSize == 64}">selected="selected"</c:if>>64</option>
                                    <option value="128" <c:if test="${config.albumImageSize == 128}">selected="selected"</c:if>>128</option>
                                    <option value="256" <c:if test="${config.albumImageSize == 256}">selected="selected"</c:if>>256</option>
                                    <option value="-1" <c:if test="${config.albumImageSize == -1}">selected="selected"</c:if>><fmt:message key="settings.albumImageSizeMaximum" /></option>
                                </select>
                            </td>
                        </tr>
                        <c:if test="${authUser.rss}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.playlistTypes.rss" /></td>
                                <td>
                                    <input type="checkbox" name="feedType" value="rss" <c:if test="${config.showRss}">checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-rss.png" alt="RSS" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.playlist}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.playlistTypes.playlist" /></td>
                                <td>
                                    <input type="checkbox" name="feedType" value="playlist" <c:if test="${config.showPlaylist}"> checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-playlist.png" alt="playlist" style="vertical-align:text-top;" />
                                    <!--<fmt:message key="settings.playlistType" />:-->
                                    <select name="playlistType">
                                        <option value="M3u" <c:if test="${config.playlistType eq 'M3u'}">selected="selected"</c:if>>m3u</option>
                                        <option value="Xspf" <c:if test="${config.playlistType eq 'Xspf'}">selected="selected"</c:if>>xspf</option>
                                    </select>
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.download}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showDownload" /></td>
                                <td>
                                    <input id="downloadCheckbox" type="checkbox" name="showDownload" value="true" <c:if test="${config.showDownload}">checked="checked"</c:if> onclick="toggleDownload()"/>
                                    <img src="${themeUrl}/images/action-download.png" alt="playlist" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.yahooPlayer}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showYahooMediaPlayer" /></td>
                                <td>
                                    <input id="yahooPlayerCheckbox" type="checkbox" name="showYahooMediaPlayer" value="true" <c:if test="${config.yahooMediaPlayer}">checked="checked"</c:if> <c:if test="${!config.showDownload}">disabled="disabled"</c:if> />
                                    <img src="${themeUrl}/images/action-yahoo.png" alt="playlist" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${globalConfig.flashPlayer && authUser.player}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showPlayer" /></td>
                                <td>
                                    <input type="checkbox" name="showPlayer" value="true" <c:if test="${config.showPlayer}">checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-flash.png" alt="player" style="vertical-align:text-top;" />
                                    <c:if test="${fn:length(mtfn:flashPlayerConfigs()) > 1}">
                                        <select name="flashplayer">
                                            <option value="" <c:if test="${empty config.flashplayer}">selected="selected"</c:if>>
                                                <fmt:message key="settings.flashPlayerOnTheFly"/>
                                            </option>
                                            <c:forEach items="${mtfn:flashPlayerConfigs()}" var="player">
                                                <option value="${player.id}"
                                                        <c:if test="${config.flashplayer eq player.id}">selected="selected"</c:if>>
                                                    <c:out value="${player.name}"/></option>
                                            </c:forEach>
                                        </select>
                                    </c:if>
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.remoteControl && globalConfig.remoteControl}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showRemoteControl" /></td>
                                <td>
                                    <input type="checkbox" name="remoteControl" value="true" <c:if test="${config.remoteControl}">checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-remote.png" alt="remote control" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showAddRemoteControl" /></td>
                                <td>
                                    <input type="checkbox" name="addRemoteControl" value="true" <c:if test="${config.addRemoteControl}">checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-addremote.png" alt="add to remote control" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.externalSites}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showExternalSites" /></td>
                                <td>
                                    <input type="checkbox" name="showExternalSites" value="true" <c:if test="${config.showExternalSites}">checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-links.png" alt="external links" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.createPlaylists}">
                            <tr <mt:flipFlop/>>
                                <td class="label"><fmt:message key="settings.showAddToPlaylist" /></td>
                                <td>
                                    <input type="checkbox" name="showAddToPlaylist" value="true" <c:if test="${config.showAddToPlaylist}">checked="checked"</c:if> />
                                    <img src="${themeUrl}/images/action-oneclickplaylist.png" alt="add to playlist" style="vertical-align:text-top;" />
                                </td>
                            </tr>
                        </c:if>
                        <c:if test="${authUser.transcoder && !empty globalConfig.effectiveTranscoderConfigs && !authUser.forceTranscoders && globalConfig.validVlcConfig}">
                            <c:forEach var="tc" items="${globalConfig.effectiveTranscoderConfigs}">
                                <tr <mt:flipFlop/>>
                                    <td class="label"><fmt:message key="settings.transcoder" />: <c:out value="${tc.name}" /></td>
                                    <td>
                                        <input type="checkbox" name="transcoder" value="${tc.name}" <c:if test="${mtfn:isTranscoder(config, tc)}">checked="checked"</c:if> />
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:if>
                    </table>
                    <div class="buttons">
                        <input id="linkSave" type="submit" value="<fmt:message key="doSave"/>" />
                        <input id="linkCancel" type="button" value="<fmt:message key="doCancel"/>" onclick="document.location.href='${servletUrl}/showPortal/${auth}'" />
                    </div>
                </form>

            </div>

        </div>

        <div class="footer">
            <div class="inner"></div>
        </div>

    </div>

</body>

</html>
