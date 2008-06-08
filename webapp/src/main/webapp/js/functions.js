var tooltipElement;
var mouseX;
var mouseY;

document.onmousemove = updateTooltipPosition;

function sort(servletUrl, auth, sortOrder) {
    document.forms["browse"].action = servletUrl + "/browseTrack/" + auth;
    document.forms["browse"].elements["sortOrder"].value = sortOrder;
    document.forms["browse"].submit();
}

function selectAllByLoop(prefix, first, last, checkbox) {
    for (var i = first; i <= last; i++) {
        var element = document.getElementById(prefix + i);
        if (element) {
            element.checked = checkbox.checked;
        }
    }
}

function selectAll(prefix, ids, checkbox) {
    var idArray = ids.split(",");
    for (var i = 0; i < idArray.length; i++) {
        var element = document.getElementById(prefix + idArray[i]);
        if (element) {
            element.checked = checkbox.checked;
        }
    }
}

function openPlayer(url) {
    var flashPlayer = window.open(url, "MyTunesRssFlashPlayer", "width=600,height=276,resizable=no,location=no,menubar=no,scrollbars=no,status=no,toolbar=no,hotkeys=no");
    flashPlayer.onload=function() {
        flashPlayer.document.title = self.document.title;
    }
}

function getElementParams(elements, separator) {
    var elementNames = elements.split(",");
    var buffer = '';
    for (var i = 0; i < elementNames.length; i++) {
        buffer += elementNames[i] + "=" + getElementValue(self.document.getElementById(elementNames[i]));
        if (i + 1 < elementNames.length) {
            buffer += separator;
        }
    }
    return buffer;
}

function getElementValue(element) {
    if (element != undefined) {
        if (element.type == 'text') {
            return element.value;
        }
        if (element.type == 'select-one') {
            return element.options[element.options.selectedIndex].value;
        }
    }
    return '';
}

function updateTooltipPosition(e) {
    var scrLeft = (document.documentElement && document.documentElement.scrollLeft) ? document.documentElement.scrollLeft : document.body.scrollLeft;
    var scrTop = (document.documentElement && document.documentElement.scrollTop) ? document.documentElement.scrollTop : document.body.scrollTop;
    mouseX = (document.all) ? window.event.x + scrLeft : e.pageX;
    mouseY = (document.all) ? window.event.y + scrTop : e.pageY;
    if (tooltipElement != null) {
        tooltipElement.style.position = "absolute";
        tooltipElement.style.left = (mouseX + 20) + "px";
        tooltipElement.style.top = (mouseY + 20) + "px";
    }
}

function showTooltip(element) {
    tooltipElement = document.getElementById("tooltip_" + element.id);
    if (tooltipElement != null) {
        tooltipElement.style.position = "absolute";
        tooltipElement.style.left = (mouseX + 20) + "px";
        tooltipElement.style.top = (mouseY + 20) + "px";
        tooltipElement.style.display = "block";
    }
}

function hideTooltip(element) {
    tooltipElement = document.getElementById("tooltip_" + element.id);
    if (tooltipElement != null) {
        tooltipElement.style.display = "none";
    }
}

var dialogFunctions;
var dialogElement;

function showDialog(dialogId, functions) {
    dialogFunctions = functions;
    var glasspane = document.getElementById("glasspane");
    dialogElement = document.getElementById(dialogId);
    document.body.style.overflow = "hidden";
    glasspane.style.zIndex = 999;
    glasspane.style.display = "block";
    dialogElement.style.zIndex = -1000;
    dialogElement.style.display = "block";
    var left = (document.width - dialogElement.scrollWidth) / 2;
    if (left < 0) {
        left = 0;
    }
    dialogElement.style.left = left + "px";
    var top = (window.innerHeight - dialogElement.scrollHeight) / 2;
    if (top < 0) {
        top = 0;
    }
    dialogElement.style.top = top + "px";
    dialogElement.style.zIndex = 1000;
}

function clickDialog(functionIndex) {
    var glasspane = document.getElementById("glasspane");
    dialogElement.style.display = "none";
    glasspane.style.display = "none";
    glasspane.style.zIndex = -999;
    document.body.style.overflow = "auto";
    if (dialogFunctions[functionIndex]) {
        dialogFunctions[functionIndex]();
    }
}

function editExistingPlaylist() {
    var element = document.getElementById("playlistSelection");
    document.location.href = element.options[element.selectedIndex].value;
}
