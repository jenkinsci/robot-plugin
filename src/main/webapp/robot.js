
function showSignificant(urlName, path) {
    if (urlName) {
        urlName = urlName + "/";
    } else {
        urlName = "";
    }
    document.getElementById("passfailgraph_hd").href = urlName + "graph?hd=true&zoomSignificant=true";
    document.getElementById("passfailgraph").src = urlName + "graph?zoomSignificant=true";
    document.getElementById("significantshowlink").style.display = "none";
    document.getElementById("significanthidelink").style.display = "";
    setCookie("RobotResult_zoomSignificant","true", 365, path);
}

function hideSignificant(urlName, path) {
    if (urlName) {
        urlName = urlName + "/";
    } else {
        urlName = "";
    }
    document.getElementById("passfailgraph_hd").href = urlName + "graph?hd=true&zoomSignificant=false";
    document.getElementById("passfailgraph").src = urlName + "graph?zoomSignificant=false";
    document.getElementById("significantshowlink").style.display = "";
    document.getElementById("significanthidelink").style.display = "none";
    setCookie("RobotResult_zoomSignificant","false", 365, path);
}

function setCookie(c_name,value,exdays,path) {
    var exdate=new Date();
    exdate.setDate(exdate.getDate() + exdays);
    var c_value=escape(value) + ";path=" + escape(path) + ((exdays==null) ? "" : ";expires="+exdate.toUTCString());
    document.cookie=c_name + "=" + c_value;
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
