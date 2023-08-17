console.log('IMPORT KSPAY.JS FILE!');
var pairingConfig = {
    debug: true
};
var Pairingsolution = (function (win, doc) {
    var processing = false;
    var pairingConfig = {};
    var pairingSolutionResult = {};
    var op = undefined;

    var util = {
        addEventListener: function (obj, event, fnc) {
            obj.addEventListener ? obj.addEventListener(event, fnc) : obj.attachEvent("on" + event, fnc);
        },
        removeEventListener: function (obj, event, fnc) {
            obj.removeEventListener ? obj.removeEventListener(event, fnc) : obj.detachEvent("on" + event, fnc);
        },
        documentReady: function (fnc) {
            if (document.addEventListener) {
                document.addEventListener("DOMContentLoaded", function () {
                    document.removeEventListener("DOMContentLoaded", arguments.callee, false);
                    fnc();
                }, false);
            } else if (document.attachEvent) { // Internet Explorer
                document.attachEvent("onreadystatechange", function () {
                    if (document.readyState === "complete") {
                        document.detachEvent("onreadystatechange", arguments.callee);
                        fnc();
                    }
                });
            }
        },
        getBrowserInfo: function () {
            var a = navigator.userAgent,
                b, d = a.match(/(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*(\d+)/i) || [];
            if (/trident/i.test(d[1])) return b = /\brv[ :]+(\d+)/g.exec(a) || [], {
                name: "IE ",
                version: b[1] || ""
            };
            if ("Chrome" === d[1] && (b = a.match(/\bOPR\/(\d+)/), null != b)) return {
                name: "Opera",
                version: b[1]
            };
            d = d[2] ? [d[1], d[2]] : [navigator.appName, navigator.appVersion, "-?"];
            null != (b = a.match(/version\/(\d+)/i)) && d.splice(1, 1, b[1]);
            return {
                name: d[0],
                version: d[1],
                mobile: util.isMobile()
            }
        },
        isIE: function () {
            var a = !1;
            "Microsoft Internet Explorer" == navigator.appName ? null !== /MSIE ([0-9]{1,}[\.0-9]{0,})/.exec(navigator.userAgent) && (a = parseFloat(RegExp.$1)) : "Netscape" == navigator.appName && ~navigator.appVersion.indexOf("Trident") && (a = 11);
            return a
        },
        isBrokenIE: function () {
            return !!util.isIE() && 10 > util.isIE()
        },
        isOldIE: function () {
            return !!util.isIE() && 7 >= util.isIE()
        },
        isIE8: function () {
            return !!util.isIE() && 8 == util.isIE()
        },
        isSafari: function () {
            var a = navigator.userAgent.toLowerCase(),
                b; -
            1 != a.indexOf("safari") && -1 === a.indexOf("chrome") && (b = !0);
            util.isSafari = function () {
                return b
            };
            return util.isSafari()
        },
        numberWithCommas: function (x) {
            return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        },
        isRedirectBrowser: function () {
            return !!util.isIE() && 9 > util.isIE()
        },
        isMobile: function (a) {
            var b = navigator.userAgent;
            return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|SymbianOS|Mobile Safari/i.test(b) && (a || !(b.match(/Xoom|iPad/i) || b.match(/Nexus|Android/i) && (!b.match(/Mobile/i) || !1 === 600 > window.screen.availWidth)))
        },
        isTablet: function () {
            return util.isMobile(!0)
        },
        getDomain: function (a) {
            return a ? window.location.href : window.location.hostname
        },
        getProtocol: function () {
            return window.location.protocol
        },
        getLocale: function (a) {
            return document.getElementsByTagName("html")[0].getAttribute("lang") || a && (navigator.language || navigator.browserLanguage)
        },
        noop: function () {},
        guid: function () {
            function s4() {
                return ((1 + Math.random()) * 0x10000 | 0).toString(16).substring(1);
            }
            return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
        },
        getFormatDate: function () {
            var now = new Date();
            return (now.getFullYear() + "-" + now.getMonth() + 1) + "-" + now.getDate() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + ":" + now.getMilliseconds();
        },
        log: function (logMessage) {
            if (pairingConfig.debug) console.log.apply(console, arguments);
        },
        validCreditCard: function (value) {
            if (/[^0-9-\s]+/.test(value)) return false;

            if (/^6/.test(value)) return true;
            var nCheck = 0,
                nDigit = 0,
                bEven = false;
            value = value.replace(/\D/g, "");
            for (var n = value.length - 1; n >= 0; n--) {
                var cDigit = value.charAt(n),
                    nDigit = parseInt(cDigit, 10);
                if (bEven) {
                    if ((nDigit *= 2) > 9) nDigit -= 9;
                }
                nCheck += nDigit;
                bEven = !bEven;
            }
            return (nCheck % 10) == 0;
        },
        validateEmail: function (email) {
            var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            return re.test(email);
        },
        postAjax: function (url, data, success, fail) {
            // ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
            var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xhr.open('POST', url);
            xhr.onreadystatechange = function () {
                if (xhr.readyState > 3 && xhr.status == 200) {
                    success(JSON.parse(xhr.responseText));
                } else if (xhr.readyState > 3 && xhr.status != 200) {
                    fail(xhr);
                }
            };
            xhr.setRequestHeader("Accept", "application/json");
            xhr.setRequestHeader("Accept-Language", "ko_KR");
            xhr.setRequestHeader("Authorization", pairingConfig.publicKey);
            xhr.setRequestHeader("Content-Type", "application/json");
            xhr.send(data);
            return xhr;
        },
        getAjax: function (url, success, error) {
            var xhr = new XMLHttpRequest();
            if (!('withCredentials' in xhr)) xhr = new XDomainRequest(); // fix IE8/9
            xhr.open('GET', url);
            xhr.onreadystatechange = function () {
                if (xhr.readyState > 3 && xhr.status == 200) {
                    success(xhr.responseText);
                } else if (xhr.readyState > 3 && xhr.status != 200) {
                    error(xhr);
                }
            }
            xhr.send();
            return xhr;
        },
        sendMessageToParent: function (obj) {
            if (window.opener) {
                console.log('SEND POSTMESSAGE TO OPENER', obj);
                util.log('SEND POSTMESSAGE TO OPENER', obj);
                window.opener.postMessage(JSON.stringify(obj), "*");
            } else {
                console.log('SEND POSTMESSAGE TO PARENT', obj);
                util.log('SEND POSTMESSAGE TO PARENT', obj);
                window.parent.postMessage(JSON.stringify(obj), "*");
            }
        },
        loadImg: function (id, URL, title) {
            var tester = document.getElementById(id);
            tester.onerror = function () {
                tester.src = "/static/img/logo.png";
            }
            tester.title = title;
            tester.src = URL;
        }
    } // <<---------- Util END

    function fadeInEffect(id) {
        var fadeTarget = document.getElementById(id);
        document.getElementById(id).style.display = 'block';
        var fadeEffect = setInterval(function () {
            if (fadeTarget == null || fadeTarget.style == null) clearInterval(fadeEffect);
            if (!fadeTarget.style.opacity) {
                fadeTarget.style.opacity = 0;
            }
            if (fadeTarget.style.opacity > 0.9) {
                clearInterval(fadeEffect);
            } else {
                fadeTarget.style.opacity = Number(fadeTarget.style.opacity) + Number(0.1);
            }
        }, 10);
    }

    function fadeOutEffect(id) {
        var fadeTarget = document.getElementById(id);
        var fadeEffect = setInterval(function () {
            if (fadeTarget == null || fadeTarget.style == null) clearInterval(fadeEffect);
            if (!fadeTarget.style.opacity) {
                fadeTarget.style.opacity = 1;
            }
            if (fadeTarget.style.opacity < 0.1) {
                clearInterval(fadeEffect);
                document.getElementById(id).style.display = 'none';
            } else {
                fadeTarget.style.opacity -= 0.1;
            }
        }, 10);
    }

    function getConfigByToken(token, successFnc, errorFnc) {
        util.getAjax('/api/3d/widget/' + token, successFnc, errorFnc);
    }

    function setForm(config) {
        var f = doc.createElement("form");
        f.setAttribute("action", config.targetUrl);
        f.setAttribute("method", "post");
        document.body.appendChild(f);
        for (key in config.form) {
            var val = config.form[key];
            var elem = document.createElement("input");
            elem.setAttribute("type", "hidden");
            elem.setAttribute("name", key);
            elem.setAttribute("value", val);
            f.appendChild(elem);
        }
        console.log('config.device', config.device);
        if(config.device != 'mobile') {
            var agent = navigator.userAgent;
            var midx = agent.indexOf("MSIE");
            var out_size = (midx != -1 && agent.charAt(midx + 5) < '7');

            var width_ = config.width;
            var height_ = out_size ? config.height + 60 : config.height;
            var left_ = screen.width;
            var top_ = screen.height;

            left_ = left_ / 2 - (width_ / 2);
            top_ = top_ / 2 - (height_ / 2);

            op = window.open('about:blank', 'AuthFrmUp', 'height=' + height_ + ',width=' + width_ + ',status=yes,scrollbars=no,resizable=no,left=' + left_ + ',top=' + top_ + '');
            if (op == null) {
                alert("팝업이 차단되어 결제를 진행할 수 없습니다.");
                return false;
            }
            var timer = setInterval(function() {
                if(!op || op.closed) {
                    var obj = { type: 'PAY_CLOSE' };
                    util.sendMessageToParent(obj);
                    clearInterval(timer);
                }
            },3000);
            f.setAttribute("target", "AuthFrmUp");
        } else {
            f.setAttribute("target", "_top");
        }
        f.submit();
    }

    /* 팝업창으로 부터 종료 메시지 받음 */
    util.addEventListener(window, 'message', function (e) {
        util.log(e.source);
        util.log(e.origin);
        var recv = JSON.parse(e.data);
        util.log(recv);
        if (!recv.type) {} else if (recv.type === 'PAY_CLOSE') {
            util.sendMessageToParent(recv);
        }
    });

    function close(isSet) {
        fadeInEffect('c3-loading');
        var obj = {
            type: 'PAY_CLOSE'
        };
        if (isSet) obj.data = pairingSolutionResult;
        util.sendMessageToParent(obj);
        if(op != undefined && !op.closed) {
            op.close();
        }
        setTimeout(function () {
            if (self.opener) {
                self.opener = self;
                self.close();
            } else {
                window.close();
            }
        }, 500);
    }

    function validation(config) {
        pairingConfig = JSON.parse(config).widget;
        pairingConfig.form = JSON.parse(pairingConfig.form);

        pairingConfig.targetUrl = pairingConfig.targetUrl.replace('\u003d', '=');
        console.log('pairingConfig', pairingConfig);

        if(pairingConfig.device == 'mobile' && !pairingConfig.redirectUrl) {
            alert('결제 오류, 모바일 결제 필수값이 존재하지 않습니다.');
            return false;
        }

        return true;
    }

    util.documentReady(function () {
        util.log('search  ' + window.location.search);
        if (!window.location.search) {
            //alert('올바르지 않은 접근입니다.');
            return;
        }
        document.addEventListener('keydown', function (event) {
            if (event.keyCode === 13) {
                event.preventDefault();
            }
        }, true);
        var search = window.location.search.split('&');
        var token = search[0].split('=')[1];
        getConfigByToken(token, function (res) {
            if(!validation(res)) {
                return;
            }
            setForm(pairingConfig);
            /* loading hide */
            setTimeout(function () {
                fadeOutEffect('c3-loading');
            }, 800);
        }, function (err) {
            util.log('TOKEN ERROR  ', err);
            if (err.status == '401') {
                payFail({
                    advanceMsg: '서버에 접속할 수 없습니다.\nAPI키가 유효하지 않습니다',
                    resultCd: '401'
                });
                setTimeout(function () {
                    fadeOutEffect('c3-loading');
                }, 500);
            }
        });
    });

})(window, document);

/* 팝업창에서 부모창으로 데이터 보내는 로직 */
function kspayToParent() {
    var resObj = document.forms.frm.data.value;
    console.log('kspayToParent: ', resObj, decodeURIComponent(resObj));
    //close(true);
    setTimeout(function () {
        var obj = {
            type: 'PAY_CLOSE'
        };

        obj.data = decodeURIComponent(resObj);
        if (window.opener) {
            console.log('SEND POSTMESSAGE TO OPENER', obj);
            window.opener.postMessage(JSON.stringify(obj), "*");
        } else {
            console.log('SEND POSTMESSAGE TO PARENT', obj);
            window.parent.postMessage(JSON.stringify(obj), "*");
        }
        setTimeout(function () {
            if (self.opener) {
                self.opener = self;
                self.close();
            } else {
                window.close();
            }
        }, 500);
    }, 500);
}