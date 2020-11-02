// =====================================================
// Ready function (without jquery)
// =====================================================
function ready(fn) {
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

function fileUpload(form){
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {
            refreshPage();
        }
    };
    xhr.open('POST', form.action);
    xhr.withCredentials = true;
    xhr.send(new FormData(form));
}

function disableUI() {
    const fileElement = document.getElementById("file-upload");
    const submitButtonElement = document.getElementById("submit");
    fileElement.before(
        "<div id=\"processing\" aria-live=\"polite\" class=\"govuk-!-margin-bottom-5\">" +
        "<h2 class=\"govuk-heading-m\">We are checking your file, please wait</h2>" +
        "<div><div class=\"ccms-loader\">TEST TEST</div></div></div>"
    );
    fileElement.attr('disabled', 'disabled');
    submitButtonElement.addClass('govuk-button--disabled')
}

// =====================================================
// Dac6Upload Refresh status page
// =====================================================
// reportStatus element preserves the status
function refreshPage() {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (this.readyState === XMLHttpRequest.DONE) {
            const dac6UploadStatusElement            = document.getElementById("dac6UploadStatus");
            if (this.status === 200) {
                var data = JSON.parse(this.response);
                if (dac6UploadStatusElement.val() !== data.status) {
                    console.debug("status changed, updating page", data.status);

                    //Modify DOM
                    // dac6UploadStatusElement.html($(data.statusPanel));  TODO is it necessary?
                    dac6UploadStatusElement.val(data.status);

                    console.debug("page updated");
                    if (data.status === "Failed" || data.status === "Submitted" || data.status === "Done") {
                        console.debug("Reached final status, removing refresh", status);
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
    xhr.open('GET', refreshUrl);
    xhr.send();

}

ready(function(){

    const dac6UploadFormRef        = document.getElementById("dac6UploadForm");
    const dac6UploadRefreshUrl     = document.getElementById("dac6UploadRefreshUrl");

    // =====================================================
    // Upscan upload
    // =====================================================
    dac6UploadFormRef.addEventListener("submit", function(e){
        e.preventDefault();
        fileUpload(dac6UploadFormRef);
        disableUI();
    });

    // =====================================================
    // WebForm Confirmation Refresh status page
    // var data = JSON.parse(this.response);
    // =====================================================
    const refreshUrl = dac6UploadRefreshUrl.val(); // show results
    if (refreshUrl) {
        window.refreshIntervalId = setInterval(function () {
            console.debug("scheduling ajax call, refreshUrl", refreshUrl);
            refreshPage();
        }, 3000);
        console.log("intervalRefreshScheduled, id: ", window.refreshIntervalId);
    }

});
