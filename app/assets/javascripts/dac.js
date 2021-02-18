// initialise GovUK lib
GOVUKFrontend.initAll();

// =====================================================
// Handle the UR recruitmentBanner
// =====================================================

recruitmentBanner()

// =====================================================
// Back link mimics browser back functionality
// =====================================================
// store referrer value to cater for IE - https://developer.microsoft.com/en-us/microsoft-edge/platform/issues/10474810/  */
var docReferrer = document.referrer
// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
    window.history.replaceState(null, null, window.location.href);
}
// back click handle, dependent upon presence of referrer & no host change
var backLink = document.querySelector('.govuk-back-link');
if(backLink){
      backLink.addEventListener('click', function(e){
            e.preventDefault();
            if (window.history && window.history.back && typeof window.history.back === 'function' &&
            (docReferrer !== "" && docReferrer.indexOf(window.location.host) !== -1)) {
        window.history.back();
        }
  });
}

if (document.getElementById('printLink')){
    document.getElementById('printLink').addEventListener('click', function(e){
        e.preventDefault();
        window.print();
    });
}

function recruitmentBanner(){
    const recruitmentBanner = document.querySelector('#recruitment-banner');

    if(recruitmentBanner){
        const recruitmentBannerDismiss = document.querySelector('#recruitment-banner-dismiss');

        const recruitmentCookieName = "mtdpurr"
        const hasDismissed = getCookie(recruitmentCookieName)

        if (hasDismissed) {
            recruitmentBanner.remove()
        } else {
            recruitmentBannerDismiss.addEventListener('click', function(e){
                event.preventDefault()
                setCookie(recruitmentCookieName, 'suppress_for_all_services', { days: 90 })
                recruitmentBanner.remove()
            })
            recruitmentBanner.classList.remove('js-hidden');
        }
    }
}

function setCookie(name, value, duration, domain) {
    var secure = window.location.protocol.indexOf('https') ? '' : '; secure'
    var cookieDomain = ''
    var expires = ''

    if (domain) {
        cookieDomain = '; domain=' + domain
    }

    if (duration) {
        var date = new Date()
        date.setTime(date.getTime() + (duration.days * 24 * 60 * 60 * 1000))
        expires = '; expires=' + date.toGMTString()
    }

    document.cookie = name + '=' + value + expires + cookieDomain + '; path=/' + secure
}

function getCookie(name) {
    var i, c
    var nameEQ = name + '='
    var ca = document.cookie.split(';')

    for (i = 0; i < ca.length; i += 1) {
        c = ca[i]

        while (c.charAt(0) === ' ') {
            c = c.substring(1, c.length)
        }

        if (c.indexOf(nameEQ) === 0) {
            return c.substring(nameEQ.length, c.length)
        }
    }

    return null
}