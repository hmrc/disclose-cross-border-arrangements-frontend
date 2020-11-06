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

function startSpinner() {

    const fileElement = document.getElementById("file-upload");
    const submitButtonElement = document.getElementById("submit");
    const spinnerHtml = "<div id=\"processing\" aria-live=\"polite\" class=\"govuk-!-margin-bottom-5\">" +
        "<h2 class=\"govuk-heading-m\">We are checking your file, please wait</h2>" +
        "<div><div class=\"ccms-loader\"></div></div></div>";
    fileElement.insertAdjacentHTML('beforebegin', spinnerHtml);
    fileElement.setAttribute('disabled', 'disabled');
    submitButtonElement.classList.add('govuk-button--disabled')
}


function refreshStatusPage(url) {

    // Possible scenarios:
    // 4 - Upscan time out TODO
    // 6 - file too big    TODO
    // 5 - virus
    // 3 - invalid xml     TODO
    // 2 - invalid data    TODO

    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {

        if (this.readyState === XMLHttpRequest.DONE) {

            const dac6UploadSuccessUrl     = document.getElementById("dac6UploadSuccessUrl");
            const dac6UploadStatusElement  = document.getElementById("dac6UploadStatus");
            const dac6UploadErrorUrl       = document.getElementById("dac6UploadErrorUrl");
            const dac6VirusErrorUrl        = document.getElementById("dac6VirusErrorUrl");

            if (this.status === 200) {

                const data = JSON.parse(this.response);
                if (dac6UploadStatusElement.value !== data._type) {

                    dac6UploadStatusElement.value = data._type;

                    if (data._type === "UploadedSuccessfully") {

                        window.location.assign(dac6UploadSuccessUrl.value);
                    }
                    else if (data._type === "Quarantined") {

                        window.location.assign(dac6VirusErrorUrl.value);
                    }
                    else if (data._type === "Failed") {

                        window.location.assign(dac6VirusErrorUrl.value);
                    }
                }
            } else {

                window.location.assign(dac6UploadErrorUrl.value);
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

    // =====================================================
    // Bind submit function to the upload form
    // =====================================================
    dac6UploadFormRef.addEventListener("submit", function(e) {

        e.preventDefault();
        const xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function() {

            if (url) {

                window.refreshIntervalId = setInterval(function () {

                    refreshStatusPage(dac6UploadRefreshUrl.value);
                }, 3000);
            }
        };
        xhr.open('POST', dac6UploadFormRef.action);
        xhr.withCredentials = true;
        xhr.send(new FormData(dac6UploadFormRef));
        startSpinner();

    });

});
