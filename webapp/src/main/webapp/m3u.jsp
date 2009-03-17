<%@ page contentType="audio/x-mpegurl;charset=UTF-8" language="java" %><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %><%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %><%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/tags" prefix="mt" %><%@ taglib uri="http://www.codewave.de/jsp/functions" prefix="cwfn" %><%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>#EXTM3U
<c:forEach items="${tracks}" var="item">#EXTINF:${item.time},<c:out value="${cwfn:choose(mtfn:unknown(item.originalArtist), msgUnknown, item.originalArtist)}" /> - ${item.name}
<c:out value="${mtfn:playbackLink(pageContext, item, null)}"/>
</c:forEach>
