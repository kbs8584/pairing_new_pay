console.log('IMPORT INDEX.JS FILE!');
var pairingConfig = {
    debug: true
};
var Pairingsolution = (function(win, doc) {
    var processing = false;
    var pairingConfig = {};
    var pairingSolutionResult = {};

    var util = {
        addEventListener: function(obj, event, fnc) {
            obj.addEventListener ? obj.addEventListener(event, fnc) : obj.attachEvent("on" + event, fnc);
        },
        removeEventListener: function(obj, event, fnc) {
            obj.removeEventListener ? obj.removeEventListener(event, fnc) : obj.detachEvent("on" + event, fnc);
        },
        documentReady: function(fnc) {
            if (document.addEventListener) {
                document.addEventListener("DOMContentLoaded", function() {
                    document.removeEventListener("DOMContentLoaded", arguments.callee, false);
                    fnc();
                }, false);
            } else if (document.attachEvent) { // Internet Explorer
                document.attachEvent("onreadystatechange", function() {
                    if (document.readyState === "complete") {
                        document.detachEvent("onreadystatechange", arguments.callee);
                        fnc();
                    }
                });
            }
        },
        getBrowserInfo: function() {
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
        isIE: function() {
            var a = !1;
            "Microsoft Internet Explorer" == navigator.appName ? null !== /MSIE ([0-9]{1,}[\.0-9]{0,})/.exec(navigator.userAgent) && (a = parseFloat(RegExp.$1)) : "Netscape" == navigator.appName && ~navigator.appVersion.indexOf("Trident") && (a = 11);
            return a
        },
        isBrokenIE: function() {
            return !!util.isIE() && 10 > util.isIE()
        },
        isOldIE: function() {
            return !!util.isIE() && 7 >= util.isIE()
        },
        isIE8: function() {
            return !!util.isIE() && 8 == util.isIE()
        },
        isSafari: function() {
            var a = navigator.userAgent.toLowerCase(),
                b; -
            1 != a.indexOf("safari") && -1 === a.indexOf("chrome") && (b = !0);
            util.isSafari = function() {
                return b
            };
            return util.isSafari()
        },
        numberWithCommas: function(x) {
            return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        },
        isRedirectBrowser: function() {
            return !!util.isIE() && 9 > util.isIE()
        },
        isMobile: function(a) {
            var b = navigator.userAgent;
            return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|SymbianOS|Mobile Safari/i.test(b) && (a || !(b.match(/Xoom|iPad/i) || b.match(/Nexus|Android/i) && (!b.match(/Mobile/i) || !1 === 600 > window.screen.availWidth)))
        },
        isTablet: function() {
            return util.isMobile(!0)
        },
        getDomain: function(a) {
            return a ? window.location.href : window.location.hostname
        },
        getProtocol: function() {
            return window.location.protocol
        },
        getLocale: function(a) {
            return document.getElementsByTagName("html")[0].getAttribute("lang") || a && (navigator.language || navigator.browserLanguage)
        },
        noop: function() {},
        guid: function() {
            function s4() {
                return ((1 + Math.random()) * 0x10000 | 0).toString(16).substring(1);
            }
            return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
        },
        getFormatDate: function() {
            var now = new Date();
            return (now.getFullYear() + "-" + now.getMonth() + 1) + "-" + now.getDate() + " " + now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + ":" + now.getMilliseconds();
        },
        log: function(logMessage){ if(pairingConfig.debug) console.log.apply(console, arguments); },
        validCreditCard: function(value) {
            if (/[^0-9-\s]+/.test(value)) return false;

            if(/^6/.test(value)) return true;
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
        validateEmail: function(email) {
            var re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            return re.test(email);
        },

        postAjax: function(url, data, success, fail) {
            // ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
            var xhr = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
            xhr.open('POST', url);
            xhr.onreadystatechange = function() {
                if (xhr.readyState > 3 && xhr.status == 200) {
                    success(JSON.parse(xhr.responseText));
                } else if(xhr.readyState > 3 && xhr.status != 200){
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
        getAjax: function(url, success, error) {
            var xhr = new XMLHttpRequest();
            if (!('withCredentials' in xhr)) xhr = new XDomainRequest(); // fix IE8/9
            xhr.open('GET', url);
            xhr.onreadystatechange = function() {
                if (xhr.readyState > 3 && xhr.status == 200) {
                    success(xhr.responseText);
                } else if(xhr.readyState > 3 && xhr.status != 200){
                    error(xhr);
                }
            }
            xhr.send();
            return xhr;
        },
        sendMessageToParent: function(obj) {
            if(window.opener) {
              util.log('SEND POSTMESSAGE TO OPENER', obj);
              window.opener.postMessage(JSON.stringify(obj), "*");
            }
            else {
              util.log('SEND POSTMESSAGE TO PARENT', obj);
              window.parent.postMessage(JSON.stringify(obj), "*");
            }
        },
        loadImg: function(id, URL, title) {
            var tester = document.getElementById(id);
            tester.onerror= function () {
                tester.src = "/static/img/logo.png";
            }
            tester.title = title;
            tester.src=URL;
        }
    } // <<---------- Util END

    function getConfigByToken(token, successFnc, errorFnc) {
        util.getAjax('/api/widget/' + token, successFnc, errorFnc);
    }

    function setForm(config) {
        document.getElementById('c3-amount').innerText = util.numberWithCommas(config.amount);
        if(config.payerName == undefined) config.payerName = '';
        document.getElementById('payerName').value = config.payerName;
        if(config.payerEmail == undefined) config.payerEmail = '';
        document.getElementById('payerEmail').value = config.payerEmail;
        if(config.payerTel == undefined) config.payerTel = '';
        document.getElementById('payerTel').value = config.payerTel.replace(/[^0-9]/g, '');
        /* semiAuth 입력 항목 표시 */
        if (config.semiAuth == 'Y') {
            document.getElementsByClassName('semi-auth')[0].style.display = 'block';
        }

        if (config.products.length > 0) {
            document.getElementById('c3-product-tag').innerText = config.products[0].name + (config.products.length > 1 ? (' 외 ' + config.products.length + '개') : '');
        }

        /* 연도 옵션 추가 */
        var year = new Date().getFullYear();
        for (i = 0; i < 11; i++) {
            var opt = document.createElement('option');
            opt.value = (year + i - 2000);
            opt.innerText = (year + i);
            document.getElementById('expiry-year').appendChild(opt);
        }
        /* 할부 옵션 추가 */
        for (i = 0; i <= config.apiMaxInstall; i++) {
            if (i == 1) continue;
            if (i > 1 && config.amount < 50000) break;
            
            var opt = document.createElement('option');
            opt.value = i;
            opt.innerText = i == 0 ? '일시불' : i + ' 개월';
            document.getElementById('installment').appendChild(opt);
        }
        initCheckboxes(document.querySelectorAll('.checkbox'));

        util.loadImg('c3-logo-img', pairingConfig.widgetLogoUrl, pairingConfig.nick);
        document.getElementById('card').focus();
    }

    function inputError(id, flag) {
      var cn = document.getElementById(id).className;
      cn = cn.replace('c3-error-input', '');
      if(flag == true) cn += ' c3-error-input';
      document.getElementById(id).className = cn;
    }

    function validation(pay) {
        if(document.getElementById('c3-terms-1').getAttribute('data-checked') != 'true') {
            document.getElementById('c3-error-text').innerText = '이용약관에 동의해야 합니다.';
            return false;
        }else if(document.getElementById('c3-terms-2').getAttribute('data-checked') != 'true') {
            document.getElementById('c3-error-text').innerText = '개인정보처리방침에 동의해야 합니다.';
            return false;
        }

        var card = document.getElementById('card').value.replace(/\s/g, '');
        //if (card.length < 15 || !util.validCreditCard(card)) {
        // 2022-03-16 카드번호 자릿수 체크로만 변경처리
    	if (card.length < 15) {
            document.getElementById('c3-error-text').innerText = '카드번호가 올바르지 않습니다.';
            inputError('card', true);
            return false;
        } else inputError('card', false);

        if (pairingConfig.semiAuth == 'Y') {
            var authPw = document.getElementById('authPw').value;
            if (!authPw || !/^([0-9]{2})$/.test(authPw)) {
                document.getElementById('c3-error-text').innerText = '비밀번호 앞 2자리가 올바르지 않습니다.';
                inputError('authPw', true);
                return false;
            } else  inputError('authPw', false);
            var authDob = document.getElementById('authDob').value;
            //if (!authDob || !/^([0-9]{6})$/.test(authDob)) {
        	if (!authDob || (!/^([0-9]{6})$/.test(authDob) && !/^([0-9]{10})$/.test(authDob))) {
        		document.getElementById('c3-error-text').innerText = '생년월일 또는 사업자번호가 올바르지 않습니다.';
                inputError('authDob', true);
                return false;
            } else inputError('authDob', false);
        }

        var payerName = document.getElementById('payerName').value;
        if (!payerName || payerName.length < 2) {
            document.getElementById('c3-error-text').innerText = '구매자 성명을 입력해야 합니다.';
            inputError('payerName', true);
            return false;
        } else inputError('payerName', false);

        /* var payerEmail = document.getElementById('payerEmail').value;
        if (!payerEmail) {
            alert('구매자 Email을 입력해야 합니다.');
            return false;
        } else if (!util.validateEmail(payerEmail)) {
            alert('구매자 Email형식을 올바르게 입력해야 합니다.');
            return false;
        } */

        var payerTel = document.getElementById('payerTel').value;
        if (!payerTel || !/^([0-9\-]{8,14})$/.test(payerTel)) {
            document.getElementById('c3-error-text').innerText = '구매자 전화번호를 입력해야 합니다.';
            inputError('payerTel', true);
            return false;
        } else inputError('payerTel', false);

        document.getElementById('c3-error-text').innerText = '';
        return true;
    }

    function getPay() {
        var pay = {};
        pay.trxType = "ONTR";
        pay.tmnId = pairingConfig.tmnId;
        pay.trackId = pairingConfig.trackId ? pairingConfig.trackId : "TRACK-" + new Date().getTime();
        pay.amount = pairingConfig.amount;
        pay.payerName = document.getElementById('payerName').value;
        pay.payerEmail = document.getElementById('payerEmail').value;
        pay.payerTel = document.getElementById('payerTel').value;
        pay.udf1 = pairingConfig.udf1;
        pay.udf2 = pairingConfig.udf2;
        pay.webhookUrl = pairingConfig.webhookUrl;
        pay.products = pairingConfig.products;

        pay.card = {};
        pay.card.number = document.getElementById('card').value.replace(/\s/g, '');
        pay.card.expiry = document.getElementById('expiry-year').value + '' + document.getElementById('expiry-month').value;
        pay.card.installment = document.getElementById('installment').value;

        pay.metadata = {};
        pay.metadata.cardAuth = pairingConfig.semiAuth == 'Y' ? "true" : "false";
        pay.metadata.authPw = document.getElementById('authPw').value;
        pay.metadata.authDob = document.getElementById('authDob').value;
        
        console.log('pay : ' + pay);
        console.log('pay tmnId : ' + pay.tmnId);
        pay.salesId = pairingConfig.salesId;
        console.log('pay salesId : ' + pay.salesId);
        
        return { pay: pay };
    }
    
    document.getElementById("c3-btn-pay").addEventListener("click", payStart); /* 결제 버튼 클릭 */
    document.getElementById('authDob').addEventListener('input', onNumberMax);
    document.getElementById("c3-btn-ok").addEventListener("click", okBtnClick); /* 확인 버튼 클릭 */
    document.getElementById("c3-btn-close").addEventListener("click", okBtnExit); /* 종료 버튼 클릭 */
    document.getElementById("card").addEventListener("keypress", onCardKeyup); /* 종료 버튼 클릭 */
    document.getElementById("c3-terms-all").addEventListener("click", onCheckboxAll); /* 약관 - 전체동의 클릭 */
    document.getElementById("c3-view-terms-1").addEventListener("click", onOpenTerms1); /* 약관 보기 1  */
    document.getElementById("c3-view-terms-2").addEventListener("click", onOpenTerms2); /* 약관 보기 2  */
    document.getElementById("c3-btn-terms").addEventListener("click", onConfirmTerms); /* 약관 보기 완료  */
    
    function payStart(_this) {
        /* 1. 버튼 + 입력창 사용 불가 */
        if (processing) return;
        setProcessing(true);
        fadeInEffect('c3-loading');
        var pay = getPay();
        setTimeout(function() {
            fadeOutEffect('c3-loading');
        },2000);
        if (validation(pay)) {
            util.postAjax('/api/pay', JSON.stringify(pay), function(res) {
                util.log('PAY RES: ', res);
                pairingSolutionResult = res;
              if (res.result.resultCd == '0000') {
                paySuccess(res.pay);
              } else {
                util.log(res.result);
                payFail(res.result);
              }
			  fadeOutEffect('c3-loading');
			  setTimeout(function() {
				okBtnClick();
			  }, 3000);
            }, function(xhr) {
              util.log('ERROR :', xhr.responseText);
              payFail({advanceMsg: '결제를 완료할 수 없습니다.', resultCd: '402'});
              //alert(xhr.responseText);
              /* 팝업창을 띄우고나서... */
              setProcessing(false);
              fadeOutEffect('c3-loading');
            });
        } else {
          setProcessing(false);
          fadeOutEffect('c3-loading');
        }

        return true;
    }

    /* 결제 성공 */
    function paySuccess(pay) {
      layerReset();
      //document.getElementById('c3-alert').style.display = 'block';
      document.getElementById('c3-alert-success').style.display = 'block';
      document.getElementById('c3-alert-success-wrapper').style.display = 'block';

      document.getElementById('c3-alert-success-amount').innerText = util.numberWithCommas(pay.amount);
      document.getElementById('c3-alert-success-trxId').innerText = pay.trxId;
      document.getElementById('c3-alert-success-trxDate').innerText = new Date().getFullYear() + '-' + (new Date().getMonth()+1) + '-' + new Date().getDate() + ' ' + new Date().getHours() + ':' + new Date().getMinutes() + ':' + new Date().getSeconds();
      document.getElementById('c3-alert-success-authCd').innerText = pay.authCd;
      document.getElementById('c3-alert-success-card').innerText = pay.card.bin + '******' + pay.card.last4;
      document.getElementById('c3-alert-success-issuer').innerText = pay.card.issuer + ' 카드';

      fadeInEffect('c3-alert');
    }
    /* 결제 실패 */
    function payFail(result){
        layerReset();
        //util.log('payFail(result)' , result);
        //document.getElementById('c3-alert').style.display = 'block';
        document.getElementById('c3-alert-error').style.display = 'block';
        document.getElementById('c3-alert-error-wrapper').style.display = 'block';
        document.getElementById('c3-alert-error-msg').innerText = result.advanceMsg + ' (' + result.resultCd + ')';
        
        fadeInEffect('c3-alert');
    }

    /* 결과 레이어창 초기화 */
    function layerReset() {
        document.getElementById('c3-alert').style.opacity = 0;
        document.getElementById('c3-alert').style.display = 'none';
        document.getElementById('c3-alert-success').style.display = 'none';
        document.getElementById('c3-alert-success-wrapper').style.display = 'none';
        document.getElementById('c3-alert-error').style.display = 'none';
        document.getElementById('c3-alert-error-wrapper').style.display = 'none';
    }

    function okBtnClick() {
      if(!pairingSolutionResult.result) {
        /* 오류로 실패 */
        close(false);
      } else if(pairingSolutionResult.result.resultCd == '0000') {
        /* 결제성공 */
        close(true);
      } else { /* 결제실패 */
        fadeOutEffect('c3-alert');
        setTimeout(function() {
            layerReset();
            setProcessing(false);
        },200);
      }
    }
    function okBtnExit() {
        close(false);
    }

    function close(isSet) {
        fadeInEffect('c3-loading');
        var obj = {
            type: 'PAY_CLOSE'
        };
        if(isSet) obj.data = pairingSolutionResult;
        util.sendMessageToParent(obj);
        setTimeout(function() {
            if(self.opener) {
                self.opener = self;
                self.close();
            } else {
                window.close();
            }
        }, 500);
    }
    function onNumberMax(object) {
        if (object.target.value.length > object.target.maxLength){
            object.target.value = object.target.value.slice(0, object.target.maxLength);
        }
    }

    function onCardKeyup(event) {
      if(event.which != 8 && isNaN(String.fromCharCode(event.which))){
        event.preventDefault(); //stop character from entering input
      }
      var x = document.getElementById("card").value;
      intoSpace(x, 4);
      if(/^3[47]/.test(x)) {
        intoSpace(x, 11);
      } else {
        intoSpace(x, 9);
        intoSpace(x, 14);
      }
      
      function intoSpace(x, pos) {
        if(x.length == pos) document.getElementById("card").value = x.substring(0,pos) + ' ' + x.substring(pos,pos+1);
      }
    }

    function setProcessing(flag, tag) {
        if (flag) {
            processing = true;
        } else {
          setTimeout(function() {
            processing = false;
          }, 1000);
        }
    }

    function fadeInEffect(id) {
        var fadeTarget = document.getElementById(id);
        document.getElementById(id).style.display = 'block';
        var fadeEffect = setInterval(function () {
            if(fadeTarget == null || fadeTarget.style == null) clearInterval(fadeEffect);
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
            if(fadeTarget == null || fadeTarget.style == null) clearInterval(fadeEffect);
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
    
    function initCheckboxes(elems) {
        for (var i = 0; i < elems.length; i++) {
            new Checkbox(elems[i]);
        }

        function Checkbox(elem) {
            this.elem = elem;
            this.checked = elem.dataset.checked;
    
            elem.addEventListener('click', function() {
                this.checked = !this.checked;
                this.setAttribute('data-checked', this.checked);
            });
        }
    }
    function onCheckboxAll() {
        setTimeout(function() {
            var all = document.getElementById('c3-terms-all').getAttribute('data-checked');
            all = all == 'true' ? true : false;
            var term1 = document.getElementById('c3-terms-1');
            var term2 = document.getElementById('c3-terms-2');
            Checkbox(term1, all);
            Checkbox(term2, all);
            function Checkbox(elem, flag) {
                this.elem = elem;
                elem.checked = flag;
                this.elem.setAttribute('data-checked', flag);
            }
        }, 50);
    }
    function onOpenTerms1() {
        document.getElementById('c3-terms').style.display.opacity = 1;
        document.getElementById('c3-terms1-wrapper').style.display = 'block';
        document.getElementById('c3-terms2-wrapper').style.display = 'none';
        fadeInEffect('c3-terms');
    }
    function onOpenTerms2() {
        document.getElementById('c3-terms1-wrapper').style.display = 'none';
        document.getElementById('c3-terms2-wrapper').style.display = 'block';
        fadeInEffect('c3-terms');
    }
    function onConfirmTerms() {
        fadeOutEffect('c3-terms');
        document.getElementById('c3-terms').style.display = 'none';
    }

    util.documentReady(function() {
        util.log('search  ' + window.location.search);
        if (!window.location.search) {
            alert('올바르지 않은 접근입니다.');
            return;
        }
        document.addEventListener('keydown', function(event) {
            if (event.keyCode === 13) {
                event.preventDefault();
            }
        }, true);
        var token = window.location.search.split('=')[1];
        getConfigByToken(token, function(res) {
            pairingConfig = JSON.parse(res).widget;
            console.log('pairingConfig', pairingConfig);
            setForm(pairingConfig);
            /* loading hide */
            setTimeout(function() {
                fadeOutEffect('c3-loading');
            },800);
        }, function(err) {
            util.log('TOKEN ERROR  ', err);
            if(err.status == '401') {
                payFail({advanceMsg: '서버에 접속할 수 없습니다.\nAPI키가 유효하지 않습니다', resultCd: '401'});
                setTimeout(function() {
                    fadeOutEffect('c3-loading');
                },500);
            }
        });
    });
})(window, document);