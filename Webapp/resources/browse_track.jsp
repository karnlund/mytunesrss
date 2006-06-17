<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>

<fmt:setBundle basename="de.codewave.mytunesrss.MyTunesRSSWeb" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

<c:set var="backUrl" scope="request">${servletUrl}/browseTrack?playlist=${param.playlist}&amp;fullAlbums=${param.fullAlbums}&amp;album=${param.album}&amp;artist=${param.artist}&amp;searchTerm=${param.searchTerm}&amp;index=${param.index}&amp;backUrl=${cwfn:urlEncode(param.backUrl, 'UTF-8')}&amp;sortOrder=${sortOrder}</c:set>

<html xmlns="http://www.w3.org/1999/xhtml">

<head>

    <title><fmt:message key="applicationTitle" /> v${cwfn:sysprop('mytunesrss.version')}</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <link rel="stylesheet" type="text/css" href="${appUrl}/styles/mytunesrss.css" />
    <!--[if IE]>
      <link rel="stylesheet" type="text/css" href="${appUrl}/styles/ie.css" />
    <![endif]-->
		<script src="${appUrl}/js/functions.js" type="text/javascript"></script>

</head>

<body onload="registerTR()">

<div class="body">

<h1 class="<c:choose><c:when test="${!empty param.searchTerm}">searchResult</c:when><c:otherwise>browse</c:otherwise></c:choose>">
    <a class="portal" href="${servletUrl}/showPortal"><fmt:message key="portal"/></a> <span><fmt:message key="myTunesRss"/></span>
</h1>

<jsp:include page="/incl_error.jsp" />

<ul class="links">
    <c:if test="${sortOrderLink}">
        <li>
            <c:if test="${sortOrder != 'Album'}"><a href="#" onclick="sort('${servletUrl}', 'Album')"><fmt:message key="groupByAlbum" /></a></c:if>
            <c:if test="${sortOrder != 'Artist'}"><a href="#" onclick="sort('${servletUrl}', 'Artist')"><fmt:message key="groupByArtist" /></a></c:if>
        </li>
    </c:if>
    <c:if test="${empty sessionScope.playlist}">
        <li>
            <a href="${servletUrl}/startNewPlaylist?backUrl=${cwfn:urlEncode(backUrl, 'UTF-8')}"><fmt:message key="newPlaylist"/></a>
        </li>
    </c:if>
    <li style="float:right;">
        <a href="${param.backUrl}"><fmt:message key="back"/></a>
    </li>
</ul>

<jsp:include page="incl_playlist.jsp" />

<form id="browse" action="${servletUrl}/addToPlaylist" method="post">

<fieldset>

<input type="hidden" name="sortOrder" value="${sortOrder}" />
<input type="hidden" name="searchTerm" value="${param.searchTerm}" />
<input type="hidden" name="album" value="${param.album}" />
<input type="hidden" name="artist" value="${param.artist}" />
<input type="hidden" name="playlist" value="${param.playlist}" />
<input type="hidden" name="backUrl" value="${param.backUrl}" />
<input type="hidden" name="fullAlbums" value="${param.fullAlbums}" />

</fieldset>

<table cellspacing="0">
<c:forEach items="${tracks}" var="track">
<c:if test="${track.newSection}">
    <c:set var="count" value="0" />
    <tr>
        <c:if test="${!empty sessionScope.playlist}">
            <th class="check"><input type="checkbox" name="none" value="none" onclick="selectAll('${track.sectionIds}',this)" />
            </th>
        </c:if>
        <th class="active" colspan="2">
            <c:choose>
                <c:when test="${sortOrder == 'Album'}">
                    <c:if test="${track.simple}">
                        <c:set var="sectionFileName">${cwfn:choose(mtfn:unknown(track.artist), '(unknown)', track.artist)} -</c:set>
                        <a href="${servletUrl}/browseAlbum?artist=<c:out value="${cwfn:urlEncode(track.artist, 'UTF-8')}"/>">
                            <c:out value="${cwfn:choose(mtfn:unknown(track.artist), '(unknown)', track.artist)}" />
                        </a> -</c:if>
                    <c:set var="sectionFileName">${sectionFileName} ${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}</c:set>
                    <c:choose>
                        <c:when test="${empty param.album}">
                            <a href="${servletUrl}/browseTrack?album=<c:out value="${cwfn:urlEncode(track.album, 'UTF-8')}"/>&amp;backUrl=${cwfn:urlEncode(backUrl, 'UTF-8')}">
                                <c:out value="${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}" />
                            </a>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}" />
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <a href="${servletUrl}/browseAlbum?artist=<c:out value="${cwfn:urlEncode(track.artist, 'UTF-8')}"/>">
                        <c:out value="${cwfn:choose(mtfn:unknown(track.artist), '(unknown)', track.artist)}" />
                    </a>
                    <c:set var="sectionFileName" value="${cwfn:choose(mtfn:unknown(track.artist), '(unknown)', track.artist)}" />
                    <c:if test="${track.simple}">
                        <c:set var="sectionFileName">${sectionFileName} - ${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}</c:set>
                        -
                        <c:choose>
                            <c:when test="${empty param.album}">
                                <a href="${servletUrl}/browseTrack?album=<c:out value="${cwfn:urlEncode(track.album, 'UTF-8')}"/>&amp;backUrl=${cwfn:urlEncode(backUrl, 'UTF-8')}">
                                    <c:out value="${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}" />
                                </a>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}" />
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                </c:otherwise>
            </c:choose>
        </th>
        <c:choose>
            <c:when test="${empty sessionScope.playlist}">
                <c:forEach items="${config.feedTypes}" var="feedType">
                    <th class="icon">
                        <a href="${servletUrl}/create${fn:toUpperCase(feedType)}/authHash=${authHash}/tracklist=${cwfn:urlEncode(track.sectionIds, 'UTF-8')}/${mtfn:cleanFileName(sectionFileName)}.${config.feedFileSuffix[feedType]}">
                            <img src="${appUrl}/images/${feedType}_th.gif" alt="${feedType}" /> </a>
                    </th>
                </c:forEach>
            </c:when>
            <c:otherwise>
                <th class="icon">
                    <a href="${servletUrl}/addToPlaylist?tracklist=${cwfn:urlEncode(track.sectionIds, 'UTF-8')}&amp;backUrl=${cwfn:urlEncode(backUrl, 'UTF-8')}">
                        <img src="${appUrl}/images/add_th.gif" alt="add" /> </a>
                </th>
            </c:otherwise>
        </c:choose>
        <c:if test="${config.showDownload}">
            <th class="icon">&nbsp;</th>
        </c:if>
    </tr>
</c:if>
<tr class="${cwfn:choose(count % 2 == 0, 'even', 'odd')}">
    <c:if test="${!empty sessionScope.playlist}">
        <td class="check">
            <input type="checkbox" id="item${track.id}" name="track" value="${track.id}" />
        </td>
    </c:if>
    <td class="artist" <c:if test="${!(sortOrder == 'Album' && !track.simple)}">colspan="2"</c:if>>
        <c:if test="${mtfn:isMovie(track)}"><img src="${appUrl}/images/movie.gif" alt="video" /></c:if>
        <c:choose>
            <c:when test="${sortOrder == 'Album'}">
                <c:if test="${track.trackNumber > 0}">${track.trackNumber} -</c:if>
                <c:out value="${cwfn:choose(mtfn:unknown(track.name), '(unknown)', track.name)}" />
            </c:when>
            <c:otherwise>
                <c:if test="${!track.simple}"><c:out value="${cwfn:choose(mtfn:unknown(track.album), '(unknown)', track.album)}" /> - </c:if>
                <c:if test="${track.trackNumber > 0}">${track.trackNumber} -</c:if>
                <c:out value="${cwfn:choose(mtfn:unknown(track.name), '(unknown)', track.name)}" />
            </c:otherwise>
        </c:choose>
    </td>
    <c:if test="${sortOrder == 'Album' && !track.simple}">
        <td>
            <a href="${servletUrl}/browseAlbum?artist=${cwfn:urlEncode(track.artist, 'UTF-8')}">
                <c:out value="${cwfn:choose(mtfn:unknown(track.artist), '(unknown)', track.artist)}" />
            </a>
        </td>
    </c:if>
    <c:choose>
        <c:when test="${empty sessionScope.playlist}">
            <c:forEach items="${config.feedTypes}" var="feedType">
                <td class="icon">
                    <a href="${servletUrl}/create${fn:toUpperCase(feedType)}/authHash=${authHash}/track=<c:out value="${cwfn:urlEncode(track.id, 'UTF-8')}"/>/${mtfn:virtualTrackName(track)}.${config.feedFileSuffix[feedType]}">
                        <img src="${appUrl}/images/${feedType}${cwfn:choose(count % 2 == 0, '', '_odd')}.gif" alt="${feedType}" /> </a>
                </td>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <td class="icon">
                <a href="${servletUrl}/addToPlaylist?track=${track.id}&amp;backUrl=${cwfn:urlEncode(backUrl, 'UTF-8')}">
                    <img src="${appUrl}/images/add${cwfn:choose(count % 2 == 0, '', '_odd')}.gif" alt="add" /> </a>
            </td>
        </c:otherwise>
    </c:choose>
    <c:if test="${config.showDownload}">
        <td class="icon">
            <a href="${servletUrl}/playTrack/authHash=${authHash}/track=${track.id}/${mtfn:virtualTrackName(track)}.${mtfn:virtualSuffix(config, track)}">
                <img src="${appUrl}/images/download${cwfn:choose(count % 2 == 0, '', '_odd')}.gif" alt="<fmt:message key="downloadTrack"/>" /></a>
        </td>
    </c:if>
</tr>
<c:set var="count" value="${count + 1}" />
</c:forEach>
</table>

<c:if test="${!empty pager}">
    <c:set var="pagerCommand"
           scope="request"
           value="${servletUrl}/browseTrack?playlist=${param.playlist}&amp;fullAlbums=${param.fullAlbums}&amp;album=${param.album}&amp;artist=${param.artist}&amp;searchTerm=${param.searchTerm}&amp;index={index}&amp;backUrl=${cwfn:urlEncode(param.backUrl, 'UTF-8')}&amp;sortOrder=${sortOrder}" />
    <c:set var="pagerCurrent" scope="request" value="${cwfn:choose(!empty param.index, param.index, '0')}" />
    <jsp:include page="incl_bottomPager.jsp" />
</c:if>

<c:if test="${!empty sessionScope.playlist}">
    <div class="buttons">
        <input type="submit"
               onclick="document.forms['browse'].action = '${servletUrl}/addToPlaylist';document.forms['browse'].elements['backUrl'].value = '${backUrl}'"
               value="<fmt:message key="addSelected"/>" />
    </div>
</c:if>

</form>

</div>

</body>

</html>