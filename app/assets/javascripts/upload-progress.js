// =====================================================
// Ready function (without jquery)
// =====================================================
function ready(fn) {
    console.log("Loading");
    if (document.readyState !== 'loading'){
        fn();
    } else if (document.addEventListener) {
        document.addEventListener('DOMContentLoaded', fn);
    } else {
        document.attachEvent('onreadystatechange', function() {
            if (document.readyState !== 'loading')
                fn();
        });
    }
}

function submitError(error, data){
    window.alert("ERROR")
    const payload = {
        code: error,
        values: [],
        errorDetail: data
    };
    const xhr = new XMLHttpRequest();
    xhr.onload = function() {
        window.location = dac6UploadFormRedirect.val();
    };
    xhr.open('POST', dac6UploadReportError.val());
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.send(JSON.stringify(payload));
}

function fileUpload(form, url){
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        console.log("File uploaded", this.readyState);
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            refreshPage(url);
        }
    };
    xhr.open('POST', form.action);
    xhr.withCredentials = true;
    xhr.send(new FormData(form));
}

function disableUI() {
    const fileElement = document.getElementById("file-upload");
    const submitButtonElement = document.getElementById("submit");
    const spinnerHtml = "<div id=\"processing\" aria-live=\"polite\" class=\"govuk-!-margin-bottom-5\">" +
        "<h2 class=\"govuk-heading-m\">We are checking your file, please wait</h2>" +
        "<div><div class=\"ccms-loader\"></div></div></div>";
    fileElement.insertAdjacentHTML('beforebegin', spinnerHtml);
    fileElement.setAttribute('disabled', 'disabled');
    submitButtonElement.classList.add('govuk-button--disabled')
}

// =====================================================
// Dac6Upload Refresh status page
// =====================================================
function refreshPage(url) {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (this.readyState === XMLHttpRequest.DONE) {
            const dac6UploadStatusElement = document.getElementById("dac6UploadStatus");
            if (this.status === 200) {
                const data = JSON.parse(this.response);
                console.log(data._type)
                console.log(dac6UploadStatusElement.value)
                if (dac6UploadStatusElement.value !== data._type) {
                    console.info("status changed, updating page", data._type);

                    dac6UploadStatusElement.value = data._type;

                    console.info("page updated");
                    if (data._type === "Failed" || data._type === "Submitted" || data._type === "UploadedSuccessfully") {
                        console.debug("Reached final status, removing refresh", data._type);
                        clearInterval(window.refreshIntervalId);
                        console.debug("interval cleared");
                    }
                } else {
                    console.debug("status didn't change, we not updating anything");
                }
            } else {
                submitError("5000", data)
            }
        }
    };
    xhr.open('GET', url);
    xhr.withCredentials = true;
    xhr.send();

}

ready(function(){

    const dac6UploadFormRef        = document.getElementById("dac6UploadForm");
    const dac6UploadRefreshUrl     = document.getElementById("dac6UploadRefreshUrl");
    const refreshUrl               = dac6UploadRefreshUrl.value;

    // =====================================================
    // Upscan upload
    // =====================================================
    dac6UploadFormRef.addEventListener("submit", function(e){
        e.preventDefault();
        fileUpload(dac6UploadFormRef, refreshUrl);
        disableUI();
    });

    // =====================================================
    // WebForm Confirmation Refresh status page
    // =====================================================
    if (refreshUrl) {
        window.refreshIntervalId = setInterval(function () {
            console.log("scheduling ajax call, refreshUrl", refreshUrl);
            refreshPage(refreshUrl);
        }, 3000);
        console.log("intervalRefreshScheduled, id: ", window.refreshIntervalId);
    }

});
