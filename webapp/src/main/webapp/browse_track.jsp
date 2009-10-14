<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/tags" prefix="mt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mttag" %>

<%--@elvariable id="appUrl" type="java.lang.String"--%>
<%--@elvariable id="servletUrl" type="java.lang.String"--%>
<%--@elvariable id="permFeedServletUrl" type="java.lang.String"--%>
<%--@elvariable id="auth" type="java.lang.String"--%>
<%--@elvariable id="encryptionKey" type="javax.crypto.SecretKey"--%>
<%--@elvariable id="authUser" type="de.codewave.mytunesrss.User"--%>
<%--@elvariable id="globalConfig" type="de.codewave.mytunesrss.MyTunesRssConfig"--%>
<%--@elvariable id="config" type="de.codewave.mytunesrss.servlet.WebConfig"--%>

<c:set var="backUrl" scope="request">${servletUrl}/browseTrack/${auth}/<mt:encrypt key="${encryptionKey}">playlist=${cwfn:encodeUrl(param.playlist)}/fullAlbums=${param.fullAlbums}/album=${cwfn:encodeUrl(param.album)}/artist=${cwfn:encodeUrl(param.artist)}/genre=${cwfn:encodeUrl(param.genre)}/searchTerm=${cwfn:encodeUrl(param.searchTerm)}/fuzzy=${cwfn:encodeUrl(param.fuzzy)}/index=${param.index}/sortOrder=${sortOrder}</mt:encrypt>/backUrl=${param.backUrl}</c:set>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <jsp:include page="incl_head.jsp"/>

</head>

<body>

<div class="body">

<h1 class="<c:choose><c:when test="${!empty param.searchTerm}">searchResult</c:when><c:otherwise>browse</c:otherwise></c:choose>">
    <a class="portal" href="${servletUrl}/showPortal/${auth}"><fmt:message key="portal"/></a> <span><fmt:message key="myTunesRss"/></span>
</h1>

<jsp:include page="/incl_error.jsp" />

<ul class="links">
    <c:if test="${sortOrderLink}">
        <li>
            <c:if test="${sortOrder != 'Album'}"><a style="cursor:pointer" onclick="sort('${servletUrl}', '${auth}', 'Album'); return false"><fmt:message key="groupByAlbum" /></a></c:if>
            <c:if test="${sortOrder != 'Artist'}"><a style="cursor:pointer" onclick="sort('${servletUrl}', '${auth}', 'Artist'); return false"><fmt:message key="groupByArtist" /></a></c:if>
        </li>
    </c:if>
    <c:if test="${!stateEditPlaylist && authUser.createPlaylists}">
        <li>
            <c:choose>
                <c:when test="${empty editablePlaylists || simpleNewPlaylist}">
                    <a href="${servletUrl}/startNewPlaylist/${auth}/backUrl=${mtfn:encode64(backUrl)}"><fmt:message key="newPlaylist"/></a>
                </c:when>
                <c:otherwise>
                    <a style="cursor:pointer" onclick="$jQ('#editPlaylistDialog').dialog('open')"><fmt:message key="editExistingPlaylist"/></a>
                </c:otherwise>
            </c:choose>
        </li>
    </c:if>
    <li style="float:right;">
        <a href="${mtfn:decode64(param.backUrl)}"><fmt:message key="back"/></a>
    </li>
</ul>

<jsp:include page="incl_playlist.jsp" />

<table cellspacing="0">
<c:set var="fnCount" value="0" />
<c:forEach items="${tracks}" var="track" varStatus="loopStatus">
<c:if test="${track.newSection}">
    <c:set var="sectionFileName" value=""/>
    <tr>
        <th id="functionsDialogName${fnCount}" class="active" colspan="2">
            <c:choose>
                <c:when test="${sortOrder == 'Album'}">
                    <c:if test="${track.simple}">
                        <c:set var="sectionFileName">${cwfn:choose(mtfn:unknown(track.artist), msgUnknown, track.artist)} -</c:set>
                        <a href="${servletUrl}/browseAlbum/${auth}/<mt:encrypt key="${encryptionKey}">artist=${cwfn:encodeUrl(mtfn:encode64(track.artist))}</mt:encrypt>">
                            <c:out value="${cwfn:choose(mtfn:unknown(track.artist), msgUnknown, track.artist)}" />
                        </a> -</c:if>
                    <c:set var="sectionFileName">${sectionFileName} ${cwfn:choose(mtfn:unknown(track.album), msgUnknown, track.album)}</c:set>
                    <c:choose>
                        <c:when test="${empty param.album}">
                            <a href="${servletUrl}/browseTrack/${auth}/<mt:encrypt key="${encryptionKey}">album=${cwfn:encodeUrl(mtfn:encode64(track.album))}</mt:encrypt>/backUrl=${mtfn:encode64(backUrl)}">
                                <c:out value="${cwfn:choose(mtfn:unknown(track.album), msgUnknown, track.album)}" />
                            </a>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${cwfn:choose(mtfn:unknown(track.album), msgUnknown, track.album)}" />
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <a href="${servletUrl}/browseAlbum/${auth}/<mt:encrypt key="${encryptionKey}">artist=${cwfn:encodeUrl(mtfn:encode64(track.artist))}</mt:encrypt>">
                        <c:out value="${cwfn:choose(mtfn:unknown(track.artist), msgUnknown, track.artist)}" />
                    </a>
                    <c:set var="sectionFileName" value="${cwfn:choose(mtfn:unknown(track.artist), msgUnknown, track.artist)}" />
                    <c:if test="${track.simple}">
                        <c:set var="sectionFileName">${sectionFileName} - ${cwfn:choose(mtfn:unknown(track.album), msgUnknown, track.album)}</c:set>
                        -
                        <c:choose>
                            <c:when test="${empty param.album}">
                                <a href="${servletUrl}/browseTrack/${auth}/<mt:encrypt key="${encryptionKey}">album=${cwfn:encodeUrl(mtfn:encode64(track.album))}</mt:encrypt>/backUrl=${mtfn:encode64(backUrl)}">
                                    <c:out value="${cwfn:choose(mtfn:unknown(track.album), msgUnknown, track.album)}" />
                                </a>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${cwfn:choose(mtfn:unknown(track.album), msgUnknown, track.album)}" />
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </th>
        <c:set var="sectionArguments"><c:choose><c:when test="${empty track.sectionPlaylistId}">tracklist=${cwfn:encodeUrl(track.sectionIds)}</c:when><c:otherwise>playlist=${cwfn:encodeUrl(track.sectionPlaylistId)}</c:otherwise></c:choose></c:set>
        <th class="icon">
            <c:choose>
                <c:when test="${!stateEditPlaylist}">
                    <mttag:actions index="${fnCount}"
                                   backUrl="${mtfn:encode64(backUrl)}"
                                   linkFragment="${sectionArguments}"
                                   filename="${mtfn:webSafeFileName(sectionFileName)}"
                                   zipFileCount="${mtfn:sectionTrackCount(track.sectionIds)}" />

                </c:when>
                <c:otherwise>
                    <a style="cursor:pointer" onclick="addTracksToPlaylist($A([${mtfn:jsArray(fn:split(track.sectionIds, ","))}]))"><img src="${appUrl}/images/add_th.gif" alt="add" /></a>
                </c:otherwise>
            </c:choose>
        </th>
    </tr>
    <c:set var="fnCount" value="${fnCount + 1}" />
</c:if>
<tr class="${cwfn:choose(loopStatus.index % 2 == 0, 'even', 'odd')}">
    <td class="artist<c:if test="${config.showThumbnailsForTracks && !empty(track.imageHash)}"> coverThumbnailColumn</c:if>" <c:if test="${!(sortOrder == 'Album' && !track.simple)}">colspan="2"</c:if>>
        <c:if test="${config.showThumbnailsForTracks && !empty(track.imageHash)}">
            <img class="coverThumbnail" id="trackthumb_${loopStatus.index}" src="${servletUrl}/showImage/${auth}/<mt:encrypt key="${encryptionKey}">hash=${track.imageHash}/size=32</mt:encrypt>" onmouseover="showTooltip(this)" onmouseout="hideTooltip(this)" alt=""/>
            <div class="tooltip" id="tooltip_trackthumb_${loopStatus.index}"><img src="${servletUrl}/showImage/${auth}/<mt:encrypt key="${encryptionKey}">hash=${track.imageHash}/size=${config.albumImageSize}</mt:encrypt>" alt=""/></div>
        </c:if>
        <c:if test="${track.protected}"><img src="${appUrl}/images/protected${cwfn:choose(loopStatus.index % 2 == 0, '', '_odd')}.gif" alt="<fmt:message key="protected"/>" style="vertical-align:middle"/></c:if>
        <c:choose>
            <c:when test="${track.source.jspName == 'YouTube'}"><img src="${appUrl}/images/youtube${cwfn:choose(loopStatus.index % 2 == 0, '', '_odd')}.png" alt="<fmt:message key="video"/>" style="vertical-align:middle"/></c:when>
            <c:when test="${track.mediaType.jspName == 'Video'}"><img src="${appUrl}/images/movie${cwfn:choose(loopStatus.index % 2 == 0, '', '_odd')}.gif" alt="<fmt:message key="video"/>" style="vertical-align:middle"/></c:when>
        </c:choose>
        <a id="functionsDialogName${fnCount}" href="${servletUrl}/showTrackInfo/${auth}/<mt:encrypt key="${encryptionKey}">track=${track.id}</mt:encrypt>/backUrl=${mtfn:encode64(backUrl)}" onmouseover="showTooltip(this)" onmouseout="hideTooltip(this)">
            <c:choose>
                <c:when test="${!empty param['playlist']}">
                    <c:if test="${!mtfn:unknown(track.artist)}"><c:out value="${track.artist}"/> -</c:if>
                    <c:out value="${cwfn:choose(mtfn:unknown(track.name), msgUnknown, track.name)}" />
                </c:when>
                <c:when test="${sortOrder == 'Album'}">
                    <c:if test="${track.trackNumber > 0}">${track.trackNumber} -</c:if>
                    <c:out value="${cwfn:choose(mtfn:unknown(track.name), msgUnknown, track.name)}" />
                </c:when>
                <c:otherwise>
                    <c:if test="${!track.simple && !mtfn:unknown(track.album)}"><c:out value="${track.album}" /> -</c:if>
                    <c:if test="${track.trackNumber > 0}">${track.trackNumber} -</c:if>
                    <c:out value="${cwfn:choose(mtfn:unknown(track.name), msgUnknown, track.name)}" />
                </c:otherwise>
            </c:choose>
            <c:if test="${!empty track.comment}">
                <div class="tooltip" id="tooltip_tracklink_${track.id}">
                    <c:forEach var="comment" varStatus="loopStatus" items="${mtfn:splitComments(track.comment)}">
                        <c:out value="${comment}"/>
                        <c:if test="${!loopStatus.last}"><br /></c:if>
                    </c:forEach>
                </div>
            </c:if>
        </a>
    </td>
    <c:if test="${sortOrder == 'Album' && !track.simple}">
        <td>
            <a href="${servletUrl}/browseAlbum/${auth}/<mt:encrypt key="${encryptionKey}">artist=${cwfn:encodeUrl(mtfn:encode64(track.artist))}</mt:encrypt>">
                <c:out value="${cwfn:choose(mtfn:unknown(track.artist), msgUnknown, track.artist)}" />
            </a>
        </td>
    </c:if>
    <td class="icon">
        <c:choose>
            <c:when test="${!stateEditPlaylist}">
                <mttag:actions index="${fnCount}"
                               backUrl="${mtfn:encode64(backUrl)}"
                               linkFragment="track=${track.id}"
                               filename="${mtfn:virtualTrackName(track)}"
                               track="${track}"
                               externalSitesFlag="${mtfn:externalSites('title') && authUser.externalSites}" />
            </c:when>
            <c:otherwise>
                <c:if test="${mtfn:lowerSuffix(config, authUser, track) eq 'mp3' && config.showDownload && authUser.download && config.yahooMediaPlayer}">
                    <c:set var="yahoo" value="true"/>
                    <a class="htrack" href="<c:out value="${mtfn:playbackLink(pageContext, track, null)}"/>"/>
                        <img src="${servletUrl}/showImage/${auth}/<mt:encrypt key="${encryptionKey}">hash=${track.imageHash}/size=64</mt:encrypt>" style="display:none" alt=""/>
                    </a>
                </c:if>
                <a style="cursor:pointer" onclick="addTracksToPlaylist($A(['${mtfn:escapeJs(track.id)}']))">
                    <img src="${appUrl}/images/add${cwfn:choose(loopStatus.index % 2 == 0, '', '_odd')}.gif" alt="add" /> </a>
            </c:otherwise>
        </c:choose>
    </td>
</tr>
<c:set var="fnCount" value="${fnCount + 1}"/>
</c:forEach>
</table>

<c:if test="${!empty pager}">
    <c:set var="pagerCommand"
           scope="request">${servletUrl}/browseTrack/${auth}/<mt:encrypt key="${encryptionKey}">playlist=${cwfn:encodeUrl(param.playlist)}/fullAlbums=${param.fullAlbums}/album=${cwfn:encodeUrl(param.album)}/artist=${cwfn:encodeUrl(param.artist)}/genre=${cwfn:encodeUrl(param.genre)}/searchTerm=${cwfn:encodeUrl(param.searchTerm)}/fuzzy=${cwfn:encodeUrl(param.fuzzy)}/sortOrder=${sortOrder}</mt:encrypt>/index={index}/backUrl=${param.backUrl}</c:set>
    <c:set var="pagerCurrent" scope="request" value="${cwfn:choose(!empty param.index, param.index, '0')}" />
    <jsp:include page="incl_bottomPager.jsp" />
</c:if>

</div>

<jsp:include page="incl_edit_playlist_dialog.jsp"/>

<c:set var="externalSiteDefinitions" scope="request" value="${mtfn:externalSiteDefinitions('title')}"/>
<jsp:include page="incl_external_sites_dialog.jsp"/>
<jsp:include page="incl_functions_menu.jsp" />

<c:if test="${yahoo}"><script type="text/javascript" src="http://mediaplayer.yahoo.com/js"></script></c:if>

</body>

</html>