// JSONViewer - by Roman Makudera 2016 (c) MIT licence.
var JSONViewer=function(L){var o={}.toString,c=o.call(new Date);function e(){this._dom_container=L.createElement("pre"),this._dom_container.classList.add("json-viewer")}function C(e,t){var n=L.createElement("span"),a=typeof e,i=""+e;return"string"===a?i='"'+e+'"':null===e?a="null":t&&(a="date",i=e.toLocaleString()),n.className="type-"+a,n.textContent=i,n}function N(e){var t=L.createElement("span");return t.className="items-ph hide",t.innerHTML=function(e){return e+" "+(1<e||0===e?"items":"item")}(e),t}function T(e){var t=L.createElement("a");return t.classList.add("list-link"),t.href="javascript:void(0)",t.innerHTML=e||"",t}return e.prototype.showJSON=function(e,t,n){var a="number"==typeof t?t:-1,i="number"==typeof n?n:-1;this._dom_container.innerHTML="",function p(e,h,u,f,g){var t=o.call(h)===c;var n=!t&&"object"==typeof h&&null!==h&&"toJSON"in h?h.toJSON():h;if("object"!=typeof n||null===n||t)e.appendChild(C(h,t));else{var a=0<=u&&u<=g,i=0<=f&&f<=g,v=Array.isArray(n),l=v?n:Object.keys(n);if(0===g){var r=N(l.length),s=T(v?"[":"{");l.length?(s.addEventListener("click",function(){a||(s.classList.toggle("collapsed"),r.classList.toggle("hide"),e.querySelector("ul").classList.toggle("hide"))}),i&&(s.classList.add("collapsed"),r.classList.remove("hide"))):s.classList.add("empty"),s.appendChild(r),e.appendChild(s)}if(l.length&&!a){var m=l.length-1,y=L.createElement("ul");y.setAttribute("data-level",g),y.classList.add("type-"+(v?"array":"object")),l.forEach(function(e,t){var n=v?e:h[e],a=L.createElement("li");if("object"==typeof n)if(!n||n instanceof Date)a.appendChild(L.createTextNode(v?"":e+": ")),a.appendChild(C(n||null,!0));else{var i=Array.isArray(n),l=i?n.length:Object.keys(n).length;if(l){var r=("string"==typeof e?e+": ":"")+(i?"[":"{"),s=T(r),d=N(l);0<=u&&u<=g+1?a.appendChild(L.createTextNode(r)):(s.appendChild(d),a.appendChild(s)),p(a,n,u,f,g+1),a.appendChild(L.createTextNode(i?"]":"}"));var o=a.querySelector("ul"),c=function(){s.classList.toggle("collapsed"),d.classList.toggle("hide"),o.classList.toggle("hide")};s.addEventListener("click",c),0<=f&&f<=g+1&&c()}else a.appendChild(L.createTextNode(e+": "+(i?"[]":"{}")))}else v||a.appendChild(L.createTextNode(e+": ")),p(a,n,u,f,g+1);t<m&&a.appendChild(L.createTextNode(",")),y.appendChild(a)},this),e.appendChild(y)}else if(l.length&&a){var d=N(l.length);d.classList.remove("hide"),e.appendChild(d)}if(0===g){if(!l.length){var d=N(0);d.classList.remove("hide"),e.appendChild(d)}e.appendChild(L.createTextNode(v?"]":"}")),i&&e.querySelector("ul").classList.add("hide")}}}(this._dom_container,e,a,i,0)},e.prototype.getContainer=function(){return this._dom_container},e}(document);
const jsonViewer = new JSONViewer();

function init()
{
	document.querySelector("#result").appendChild(jsonViewer.getContainer());
}

const error_codes = {
	"0"	: "Perhaps a <a href=https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS/Errors/CORSDidNotSucceed>CORS</a> issue",
	"200"	: "OK",
	"400"	: "Bad request",
	"401"	: "Unauthorized",
	"402"	: "Payment required",
	"403"	: "Forbidden",
	"405"	: "Method not allowed",
	"500"	: "Internal error",
	"501"	: "Not yet implemented",
};

