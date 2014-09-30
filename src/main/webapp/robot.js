function initGraph(target) {
    var mode = getCookie("RobotResult_zoom", "true");
    document.getElementById("zoomToChanges").checked = (mode == "true");
    setGraphSrc(target, mode);
}

function setGraphSrc(target, mode) {
    if (document.getElementById("passfailgraph_hd"))
        document.getElementById("passfailgraph_hd").href =  target+"graph?hd=true&zoomSignificant="+mode;
    document.getElementById("passfailgraph_hd_link").href =  target+"graph?hd=true&zoomSignificant="+mode;
    document.getElementById("passfailgraph").src =  target+"graph?zoomSignificant="+mode;
}

function zoomToChanges(target) {
    var mode = Boolean(document.getElementById('zoomToChanges').checked).toString();
    setCookie("RobotResult_zoom",mode, 365);
    setGraphSrc(target, mode);
}

function setCookie(c_name,value,exdays) {
    var exdate=new Date();
    exdate.setDate(exdate.getDate() + exdays);
    var c_value=escape(value) + ((exdays==null) ? "" : ";expires="+exdate.toUTCString());
    console.log("setting cookie: "+c_value);
    document.cookie=c_name + "=" + c_value;
}

function getCookie(c_name, default_value) {
    var name = c_name + "=";
    var cookies = document.cookie.split(';');
    for(var i=0; i < cookies.length; i++) {
        var cookie = cookies[i];
        while (cookie.charAt(0)==' ') cookie = cookie.substring(1);
        if (cookie.indexOf(name) != -1) return cookie.substring(name.length, cookie.length);
    }
    return default_value;
}

function showStackTrace(id,query) {
    var element = document.getElementById(id)
    element.style.display = "";
    document.getElementById(id + "-showlink").style.display = "none";
    document.getElementById(id + "-hidelink").style.display = "";

    var rqo = new XMLHttpRequest();
    rqo.open('GET', query, true);
    rqo.onreadystatechange = function() { element.innerHTML = rqo.responseText; }
    rqo.send(null);
}

function hideStackTrace(id) {
    document.getElementById(id).style.display = "none";
    document.getElementById(id + "-showlink").style.display = "";
    document.getElementById(id + "-hidelink").style.display = "none";
}
