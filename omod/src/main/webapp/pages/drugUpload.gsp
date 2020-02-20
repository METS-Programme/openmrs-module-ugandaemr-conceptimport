<%
    ui.decorateWith("appui", "standardEmrPage", [title: ui.message("Concept Upload")])

    def htmlSafeId = { extension ->
        "${extension.id.replace(".", "-")}-${extension.id.replace(".", "-")}-extension"
    }
    def breadcrumbMiddle = breadcrumbOverride ?: '';
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("coreapps.app.systemAdministration.label")}", link: '/' + OPENMRS_CONTEXT_PATH + '/coreapps/systemadministration/systemAdministration.page'},
        { label: "UgandaEMR Sync", link: '/' + OPENMRS_CONTEXT_PATH + '/ugandaemrsync/ugandaemrsync.page'},
        { label: "Upload Concepts Results"}
    ];
</script>
<style>
#browser_file_container {
    width: 80%;
    padding-left: 0px;
    padding-right: 0px;
}

#upload_button_container {
    width: 20%;
    text-align: right;
    padding-left: 0px;
    padding-right: 0px;
}

#browser_file {
    width: 109%;
    text-align: right;
}

.div-col2 {
    padding-left: 30px;
    padding-right: 30px;
}
</style>

<div>
    <label style="text-align: center"><h1>Concept Upload Page</h1></label>

</div>

<form method="post" id="upload_vl" enctype="multipart/form-data" accept-charset="UTF-8">
    <div class="div-table">
        <div class="div-row" id="">
            <div class="div-col2" id="browser_file_container">
                <input type="file" name="file" accept=".csv" id="browser_file"/></div>

            <div class="div-col4" id="upload_button_container"><input type="submit" value="Upload file"/></div>
        </div>
    </div>
</form>
