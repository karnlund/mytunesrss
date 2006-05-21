<%@ page contentType="audio/mpegurl;charset=UTF-8" language="java" %><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %><%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %><%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %><fmt:setBundle basename="de.codewave.mytunesrss.MyTunesRSSWeb"/>#EXTM3U
<c:forEach items="${tracks}" var="item">#EXTINF:${item.time},${item.artist} - ${item.name}
${servletUrl}/playTrack/track=${item.id}/authHash=${authHash}/${mtfn:virtualTrackName(item)}.${mtfn:virtualSuffix(config, item)}
</c:forEach>
