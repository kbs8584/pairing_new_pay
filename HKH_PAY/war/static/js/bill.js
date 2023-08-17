function setDBClickCheck(self) {
  if(self.className.indexOf('disabled') > -1) {
    console.log('disabled');
    return false;
  }
  self.className += ' disabled';

  setTimeout(function() {
    if(self.className.indexOf('disabled') > -1) {
      self.classList.remove("disabled");
      console.log('remove disabled', self.className);
    }
  },10000);
  return true;
}

function handleClickStep1(event, self) {
  if(!setDBClickCheck(self)) return;
  
  if (!document.getElementById("btn-defult-terms").checked) {
    alert("이용약관에 동의해야 합니다.");
    event.preventDefault();
    self.classList.remove("disabled");
    return;
  } else if (!document.getElementById("btn-second-terms").checked) {
    alert("자동결제 안내에 모두 동의해야 합니다.");
    event.preventDefault();
    self.classList.remove("disabled");
    return;
  }
  fadeIn(document.getElementById("loading"));
  
  moveNext(2);
}

function handleClickStep2(event, self) {
  if (!validation(event)) return;
  fadeIn(document.getElementById("loading"));
  /*
  var widget = {};
  for(var i = 0; i < document.forms.redirectForm.length; i++) {
    var input = document.forms.redirectForm[i];
    widget[input.name] = input.value;
  }
  widget.number = document.getElementById('card-input-1').value + "" + document.getElementById('card-input-2').value + "" 
  + document.getElementById('card-input-3').value + "" + document.getElementById('card-input-4').value;
  widget.expiry = document.getElementById('expire-input-2').value + "" + document.getElementById('expire-input-1').value;
  */
  var auth = {};
  auth.trxType = "card";
  auth.trackId = "AUTH" + (new Date().getTime() * (Math.floor(Math.random() * 10) +1));
  auth.recurring = true;
  auth.metadata = {authPw: "82", authDob: "820825"};
  
  var widget = {};
  for(var i = 0; i < document.forms.redirectForm.length; i++) {
	  var input = document.forms.redirectForm[i];
	  widget[input.name] = input.value;
  }
  widget.number = document.getElementById('card-input-1').value + "" + document.getElementById('card-input-2').value + "" 
  + document.getElementById('card-input-3').value + "" + document.getElementById('card-input-4').value;
  widget.expiry = document.getElementById('expire-input-2').value + "" + document.getElementById('expire-input-1').value;
  
  auth.card = widget;
  var req = JSON.stringify({auth: auth});
  console.log(req);
  var xhr = null;
  setTimeout(function() {
    if(xhr) xhr.abort();
  }, 8000);
  xhr = postAjax("/api/bill/user", req, function(res) {
  //xhr = postAjax("/api/auth", req, function(res) {
    xhr = null;
      console.log("PAY RES: ", res);
      pairingSolutionResult = res;
      if (res.result.resultCd == "0000") {
        
        moveNext(3);
      } else {
        self.classList.remove("disabled");
      }
    },
    function(xhr, timeoutEvent) {
      console.log("ERROR :", xhr.responseText, xhr, timeoutEvent);
      alert('결제에 실패했습니다. 문제가 지속적으로 발생한다면 가맹점에 문의해주세요.');
      fadeOut(document.getElementById("loading"));
      self.classList.remove("disabled");
    }
  );
}

function handleClickStep3(event, self) {
  if (!document.getElementById("btn-defult-terms").checked) {
    alert("이용약관에 동의해야 합니다.");
    event.preventDefault();
    self.classList.remove("disabled");
    return;
  } else if (!document.getElementById("btn-second-terms").checked) {
    alert("자동결제 유의사항에 동의해야 합니다.");
    event.preventDefault();
    self.classList.remove("disabled");
    return;
  }

  fadeIn(document.getElementById("loading"));

  moveNext(1);
}

function handleClickCancel(event, self) {
  console.log('cancel!!!!!');
}

/* === START === INPUT EVENT */
function handleFormChange(elem) {
  console.log('CHANGE FORM: ', elem.name, elem.value);
  document.forms.redirectForm[elem.name].value = elem.value;
  console.log('CHANGE FORM: ', document.forms.redirectForm);
}

function handleInputKeyPress(event) {
  event = event || window.event;
  if (isDelete(event)) return;
  if (isNumber(event)) {
  } else {
    event.preventDefault();
  }
}

function maxLengthCheck(object, event) {
  if (object.value.length == object.maxLength) {
    if (event.target.id == "card-input-1")
      document.getElementById("card-input-2").focus();
    if (event.target.id == "card-input-2")
      document.getElementById("card-input-3").focus();
    if (event.target.id == "card-input-3")
      document.getElementById("card-input-4").focus();
    if (event.target.id == "expire-input-1")
      document.getElementById("expire-input-2").focus();
  } else if (object.value.length > object.maxLength) {
    object.value = object.value.substring(0, object.maxLength);
  }
}

function handleInputKeyUp(event) {
  // if (!isDelete(event) && isNumber(event)) {
  //   if(String(event.target.value).length == 4) {
  //     if(event.target.id == 'card-input-1') document.getElementById('card-input-2').focus();
  //     if(event.target.id == 'card-input-2') document.getElementById('card-input-3').focus();
  //     if(event.target.id == 'card-input-3') document.getElementById('card-input-4').focus();
  //   }
  // }
}

function isDelete(event) {
  var keyID = event.which ? event.which : event.keyCode;
  return keyID == 8 || keyID == 46 || keyID == 37 || keyID == 39 || keyID == 9;
}

function isNumber(event) {
  var keyID = event.which ? event.which : event.keyCode;
  return (keyID >= 48 && keyID <= 57) || (keyID >= 96 && keyID <= 105);
}

var inputs = document.getElementsByClassName("input");
for (var i = 0; i < inputs.length; i++) {
  inputs[i].addEventListener("keydown", handleInputKeyPress);
}
for (var i = 0; i < inputs.length; i++) {
  inputs[i].addEventListener("keyup", handleInputKeyUp);
}

/* === END === INPUT EVENT */

/* Others */
function validation(event) {
  if (!document.getElementById("btn-defult-agree").checked) {
    alert("자동결제 안내에 모두 동의해야 합니다.");
    return false;
  } else if (!document.getElementById("btn-second-agree").checked) {
    alert("자동결제 안내에 모두 동의해야 합니다.");
    return false;
  }
  var cardNumber =
    document.getElementById("card-input-1").value +
    document.getElementById("card-input-2").value +
    document.getElementById("card-input-3").value +
    document.getElementById("card-input-4").value;
  if (!validCreditCard(cardNumber)) {
    alert("카드정보가 올바르지 않습니다.");
    return false;
  }
  var month = document.getElementById("expire-input-1").value;
  if (String(month).length !== 2 || month < 1 || month > 12) {
    alert("카드 유효기간(월)이 올바르지 않습니다.");
    return false;
  }
  var year = document.getElementById("expire-input-2").value;
  var nowYear = String(new Date().getFullYear()).substring(2, 4);
  if (String(year).length !== 2 || year > Number(nowYear) + 10) {
    alert("카드 유효기간(년)이 올바르지 않습니다.");
    return false;
  } else if (year < nowYear) {
    alert("카드 유효기간이 만료된 카드는 이용할 수 없습니다.");
    return false;
  }
  if (year == nowYear) {
    var nowMonth = String(new Date().getMonth() + 1);
    if (Number(month) == nowMonth) {
      alert("정기결제 카드 유효기간이 이번달인 경우 이용할 수 없습니다.");
      return false;
    } else if (Number(month) < nowMonth) {
      alert("카드 유효기간이 만료된 카드는 이용할 수 없습니다.");
      return false;
    }
  }
  return true;
}
var TIMEOUT = 10000;
function postAjax(url, data, success, fail) {
  
  // ActiveXObject를 이용한 방법은 사용할 수 없음. Flash 또는 IFrame을 이용한 전송방식을 구현 해야 함.
  var xhr = window.XMLHttpRequest
    ? new XMLHttpRequest()
    : new ActiveXObject("Microsoft.XMLHTTP");
  xhr.open("POST", url);
  xhr.setTimeout = 10000;
  xhr.onreadystatechange = function() {
    if (xhr.readyState > 3 && xhr.status == 200) {
      success(JSON.parse(xhr.responseText));
    } else if (xhr.readyState > 3 && xhr.status != 200) {
      fail(xhr);
    }
  };
  xhr.ontimeout = function(e) {
    fail(xhr, e);
  }
  xhr.setRequestHeader("Accept", "application/json");
  xhr.setRequestHeader("Accept-Language", "ko_KR");
  xhr.setRequestHeader(
    "Authorization",
    document.forms.redirectForm.payKey.value
  );
  xhr.setRequestHeader("Content-Type", "application/json");
  xhr.send(data);
  return xhr;
}

function moveNext(step) {
  setTimeout(function() {
    var all = document.getElementsByClassName("content");
    for (var i = 0; i < all.length; i++) {
      all[i].style.display = "none";
    }
    document.getElementById("container-step" + step).style.display = "block";
    fadeOut(document.getElementById("loading"));
    if (step == 2) document.getElementById("card-input-1").focus();
  }, 500);
}

function fadeOut(el) {
  el.style.opacity = 1;
  (function fade() {
    if ((el.style.opacity -= 0.1) < 0) {
      el.style.display = "none";
    } else {
      requestAnimationFrame(fade);
    }
  })();
}

function fadeIn(el, display) {
  el.style.opacity = 0;
  el.style.display = display || "block";

  (function fade() {
    var val = parseFloat(el.style.opacity);
    if (!((val += 0.1) > 1)) {
      el.style.opacity = val;
      requestAnimationFrame(fade);
    }
  })();
}

function validCreditCard(value) {
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
  return nCheck % 10 == 0;
}


if(!document.getElementById('second-terms')) {
  document.getElementById('defult-terms').style.height = '250px';
}