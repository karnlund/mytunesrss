<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>

<%--@elvariable id="config" type="de.codewave.mytunesrss.servlet.WebConfig"--%>

<div id="selectFlashPlayerDialog" class="dialog">
    <h2>
        <fmt:message key="selectFlashPlayerDialogTitle"/>
    </h2>
    <div>
        <p>
            <fmt:message key="dialog.selectFlashPlayer"/>
        </p>
        <p>
            <select id="flashPlayerSelection" style="width:100%">
                <c:forEach items="${mtfn:flashPlayerConfigs()}" var="player">
                    <option value='${player.id},${player.width},${player.height}'><c:out value="${player.name}"/></option>
                </c:forEach>
            </select>
        </p>
        <p align="right">
            <button id="linkSelectFlashPlayerOpen" onclick="doOpenPlayer()"><fmt:message key="doOpenFlashPlayer"/></button>
            <button id="linkSelectFlashPlayerCancel" onclick="$jQ.modal.close()"><fmt:message key="doCancel"/></button>
        </p>
    </div>
</div>

<script type="text/javascript">

    function doOpenPlayerWithParams(url, width, height) {
        <c:choose>
            <c:when test="${userAgent eq 'NintendoWii'}">
                self.document.location.href = url;
            </c:when>
            <c:otherwise>
                var flashPlayer = centerPopupWindow(url, "MyTunesRssFlashPlayer", width, height, "resizable=no,location=no,menubar=no,scrollbars=no,status=no,toolbar=no,hotkeys=no");
                flashPlayer.onload = function() {
                    flashPlayer.document.title = self.document.title;
                }
            </c:otherwise>
        </c:choose>
    }
    function doOpenPlayer() {
        var val = $jQ("#flashPlayerSelection option:selected").val().split(",");
        $jQ.cookie("last_mytunesrss_jukebox", val[0] + "," + val[1] + "," + val[2], {expires:100,path:"/"});
        var url = $jQ('#selectFlashPlayerDialog').data("url") + val[0];
        var width = val[1];
        var height = val[2];
        $jQ.modal.close();
        doOpenPlayerWithParams(url, width, height);
    }

    function openPlayer(url) {
        <c:choose>
            <c:when test="${empty config.flashplayer}">
                $jQ("#selectFlashPlayerDialog").data("url", url);
                $jQ("#flashPlayerSelection").val($jQ.cookie("last_mytunesrss_jukebox"));
                openDialog("#selectFlashPlayerDialog");
            </c:when>
            <c:otherwise>
                doOpenPlayerWithParams(url + '${config.flashplayer}', ${mtfn:flashPlayerConfig(config.flashplayer).width}, ${mtfn:flashPlayerConfig(config.flashplayer).height});
            </c:otherwise>
        </c:choose>
    }
</script>
