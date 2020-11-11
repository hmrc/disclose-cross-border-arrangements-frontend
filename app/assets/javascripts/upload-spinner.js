// =====================================================
// Ready function
// =====================================================

function ready(fn) {

    if (document.readyState !== 'loading') {

        fn();
    } else if (document.addEventListener) {

        document.addEventListener('DOMContentLoaded', fn);
    } else {

        document.attachEvent('onreadystatechange', function() {

            if (document.readyState !== 'loading') fn();
        });
    }
}

ready(function() {

    const dac6UploadRefreshUrl     = document.getElementById("dac6UploadRefreshUrl");

    if (dac6UploadRefreshUrl) {

        window.refreshIntervalId = setInterval(function () {

            window.location.assign(dac6UploadRefreshUrl.value);
        }, 3000);
    }

});
