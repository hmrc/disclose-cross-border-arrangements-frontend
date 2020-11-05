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

function submitError(error, data) {

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

function fileUpload(form, url) {

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {

        if (this.readyState === XMLHttpRequest.DONE && this.status === 200) {

            refreshStatusPage(url);
        }
    };
    xhr.open('POST', form.action);
    xhr.withCredentials = true;
    xhr.send(new FormData(form));
}

function startSpinner(message) {

    const fileElement = document.getElementById("file-upload");
    const submitButtonElement = document.getElementById("submit");
    const spinnerHtml = "<div id=\"processing\" aria-live=\"polite\" class=\"govuk-!-margin-bottom-5\">" +
        "<h2 class=\"govuk-heading-m\">"+ message+ "</h2>" + // TODO i18n
        "<div><div class=\"ccms-loader\"></div></div></div>";
    fileElement.insertAdjacentHTML('beforebegin', spinnerHtml);
    fileElement.setAttribute('disabled', 'disabled');
    submitButtonElement.classList.add('govuk-button--disabled')
}

function stopSpinner(message) {

    clearInterval(window.refreshIntervalId);
    const spinnerElement = document.getElementById("processing");
    const actionMessage  = "<div class=\"section\">" +
        "<a href=\"{{routes.controllers.FileValidationController.onPageLoad(uploadId.value).url}}\">" +
        "<button class=\"button\">{{messages(\"site.verify\")}}</button></a></div>";
    spinnerElement.innerHTML = message;
}

function refreshStatusPage(url) {

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
        if (this.readyState === XMLHttpRequest.DONE) {

            const dac6UploadStatusElement = document.getElementById("dac6UploadStatus");
            if (this.status === 200) {

                const data = JSON.parse(this.response);
                if (dac6UploadStatusElement.value !== data._type) {

                    dac6UploadStatusElement.value = data._type;

                    if (data._type === "UploadedSuccessfully") {

                        stopSpinner("<p>"+ data.name+ msg2.value+ "</p>");
                    }
                    else if (data._type === "Quarantined") {

                        stopSpinner("<p>"+ data.name+ msg3.value+ "</p>"); // TODO improve message
                    }
                    else if (data._type === "Failed") {

                        stopSpinner("<p>"+ data.name+ msg4.value+ "</p>");
                    }
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

ready(function() {

    const dac6UploadFormRef        = document.getElementById("dac6UploadForm");
    const dac6UploadRefreshUrl     = document.getElementById("dac6UploadRefreshUrl");
    const msg1                     = document.getElementById("msg1"); // TODO i18n messages
    const msg2                     = document.getElementById("msg2");
    const msg3                     = document.getElementById("msg3");
    const msg4                     = document.getElementById("msg4");
    const refreshUrl               = dac6UploadRefreshUrl.value;

    // =====================================================
    // Bind submit function to the upload form
    // =====================================================
    dac6UploadFormRef.addEventListener("submit", function(e) {

        e.preventDefault();
        fileUpload(dac6UploadFormRef, refreshUrl);
        startSpinner(msg1.value);
    });

    // =====================================================
    // Start page refresh
    // =====================================================
    if (refreshUrl) {

        window.refreshIntervalId = setInterval(function () {

            refreshStatusPage(refreshUrl);
        }, 3000);
    }

});
