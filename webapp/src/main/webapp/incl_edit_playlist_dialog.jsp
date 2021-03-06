<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.codewave.de/mytunesrss/jsp/functions" prefix="mtfn" %>

<c:if test="${!empty editablePlaylists}">
    <div id="editPlaylistDialog" class="dialog">
        <h2>
            <fmt:message key="editPlaylistDialogTitle"/>
        </h2>
        <div>
            <p>
                <fmt:message key="dialog.editPlaylist"/>
            </p>
            <p>
                <select id="playlistSelection" style="width:100%">
                    <c:forEach items="${editablePlaylists}" var="playlist">
                        <option value='${playlist.id}'><c:out value="${playlist.name}"/></option>
                    </c:forEach>
                </select>
            </p>
            <p align="right">
                <button id="linkPlaylistDialogCancel" onclick="$jQ.modal.close()"><fmt:message key="doCancel"/></button>
                <button id="linkPlaylistDialogEdit" onclick="editPlaylistDialog_edit()"><fmt:message key="edit"/></button>
                <button id="linkPlaylistDialogNew" onclick="editPlaylistDialog_new()"><fmt:message key="new"/></button>
            </p>
        </div>
    </div>
    <script type="text/javascript">
        function editPlaylistDialog_edit() {
            PlaylistResource.startEditPaylist({
                playlist : $jQ("#playlistSelection option:selected").val()
            });
            $jQ.modal.close();
            document.location.href = "${backUrl}";
        }
        function editPlaylistDialog_new() {
            PlaylistResource.startEditNewPlaylist();
            $jQ.modal.close();
            document.location.href = "${backUrl}";
        }
    </script>
</c:if>

<div id="addOneClickPlaylistDialog" class="dialog">
    <h2>
        <fmt:message key="editPlaylistDialogTitle"/>
    </h2>
    <div>
        <p>
            <fmt:message key="dialog.addToPlaylistOneClickSelect"/>
        </p>
        <p>
            <select id="addOneClickPlaylistDialogPlaylistSelection" style="width:100%">
                <c:forEach items="${editablePlaylists}" var="playlist">
                    <option value='${playlist.id}'><c:out value="${playlist.name}"/></option>
                </c:forEach>
            </select>
        </p>
        <p>
            <fmt:message key="dialog.addToPlaylistOneClickEnter"/>
        </p>
        <p>
            <input id="addOneClickPlaylistDialogPlaylistEnter" style="width:100%" type="text" />
        </p>
        <p align="right">
            <button id="linkOneClickPlaylistDialogCancel" onclick="$jQ.modal.close()"><fmt:message key="doCancel"/></button>
            <button id="linkOneClickPlaylistDialogAdd" onclick="addOneClickPlaylistDialog_add()"><fmt:message key="addToPlaylistOneClick"/></button>
            <button id="linkOneClickPlaylistDialogNew" onclick="addOneClickPlaylistDialog_new()"><fmt:message key="createPlaylistOneClick"/></button>
        </p>
    </div>
</div>

<script type="text/javascript">
    function addOneClickPlaylistDialog_add() {
        document.location.href = "${servletUrl}/addToOneClickPlaylist/${auth}/" + $jQ("#addOneClickPlaylistDialog").data("linkFragment") + "/playlistId=" + $jQ("#addOneClickPlaylistDialogPlaylistSelection option:selected").val() + "/backUrl=${mtfn:encode64(backUrl)}";
        $jQ.modal.close();
    }
    function addOneClickPlaylistDialog_new() {
        if ($jQ("#addOneClickPlaylistDialogPlaylistEnter").val() != '') {
            document.location.href = "${servletUrl}/addToOneClickPlaylist/${auth}/playlistName=" + escape($jQ("#addOneClickPlaylistDialogPlaylistEnter").val()) + "/" + $jQ("#addOneClickPlaylistDialog").data("linkFragment") + "/backUrl=${mtfn:encode64(backUrl)}";
            $jQ.modal.close();
        } else {
            displayError("<fmt:message key="addOneClickPlaylist.missingName"/>");
        }
    }
    function openAddOneClickPlaylistDialog(linkFragment, newPlaylistName) {
        $jQ("#addOneClickPlaylistDialog").data("linkFragment", linkFragment);
        $jQ("#addOneClickPlaylistDialogPlaylistEnter").val(newPlaylistName);
        openDialog("#addOneClickPlaylistDialog");
    }
</script>
