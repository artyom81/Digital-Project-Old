from lxml import etree

NS_SRU  = "http://www.loc.gov/zing/srw/"
NS_FCS  = "http://clarin.eu/fcs/1.0"
NS_HITS = "http://clarin.eu/fcs/dataview/1.0"
NSMAP = {"sru": NS_SRU, "fcs": NS_FCS, "hits": NS_HITS}

def _sru_root(tag: str):
    return etree.Element(etree.QName(NS_SRU, tag), nsmap=NSMAP)

def fcs_explain_xml(meta: dict, version: str = "2.0") -> bytes:
    root = _sru_root("explainResponse")
    etree.SubElement(root, etree.QName(NS_SRU, "version")).text = version

    recs = etree.SubElement(root, etree.QName(NS_SRU, "records"))
    rec  = etree.SubElement(recs, etree.QName(NS_SRU, "record"))
    etree.SubElement(rec, etree.QName(NS_SRU, "recordSchema")).text  = NS_FCS
    etree.SubElement(rec, etree.QName(NS_SRU, "recordPacking")).text = "XML"

    rdata = etree.SubElement(rec, etree.QName(NS_SRU, "recordData"))
    edesc = etree.SubElement(rdata, etree.QName(NS_FCS, "EndpointDescription"))

    # Capabilities
    caps = etree.SubElement(edesc, etree.QName(NS_FCS, "Capabilities"))
    for c in (meta.get("capabilities") or ["searchRetrieve", "explain"]):
        etree.SubElement(caps, etree.QName(NS_FCS, "capability")).text = c

    # Supported DataViews
    sdv = etree.SubElement(edesc, etree.QName(NS_FCS, "SupportedDataViews"))
    for dv in (meta.get("supported_data_views") or ["hits:kwic-1.0"]):
        etree.SubElement(sdv, etree.QName(NS_FCS, "dataView")).text = dv

    # Collections
    cols = etree.SubElement(edesc, etree.QName(NS_FCS, "Collections"))
    for col in (meta.get("collections") or []):
        c = etree.SubElement(cols, etree.QName(NS_FCS, "Collection"))
        etree.SubElement(c, etree.QName(NS_FCS, "id")).text    = str(col.get("id", "default"))
        etree.SubElement(c, etree.QName(NS_FCS, "label")).text = str(col.get("label", col.get("id","default")))

    # Languages
    langs = etree.SubElement(edesc, etree.QName(NS_FCS, "Languages"))
    for lng in (meta.get("languages") or []):
        etree.SubElement(langs, etree.QName(NS_FCS, "language")).text = lng
    if meta.get("default_language"):
        etree.SubElement(edesc, etree.QName(NS_FCS, "DefaultLanguage")).text = meta["default_language"]

    # Fields (optional, wie gehabt)
    fields = meta.get("fields") or []
    if fields:
        fld = etree.SubElement(edesc, etree.QName(NS_FCS, "Fields"))
        for f in fields:
            fx = etree.SubElement(fld, etree.QName(NS_FCS, "field"))
            etree.SubElement(fx, etree.QName(NS_FCS, "name")).text    = f.get("name","")
            etree.SubElement(fx, etree.QName(NS_FCS, "type")).text    = f.get("type","text")
            etree.SubElement(fx, etree.QName(NS_FCS, "stored")).text  = "true" if f.get("stored", True) else "false"
            etree.SubElement(fx, etree.QName(NS_FCS, "indexed")).text = "true" if f.get("indexed", True) else "false"

    # Max page size / Meta (optional)
    if meta.get("maxPageSize"):
        etree.SubElement(edesc, etree.QName(NS_FCS, "MaxPageSize")).text = str(meta["maxPageSize"])
    if meta.get("meta"):
        m = etree.SubElement(edesc, etree.QName(NS_FCS, "Meta"))
        if meta["meta"].get("rights"):
            etree.SubElement(m, etree.QName(NS_FCS, "rights")).text = meta["meta"]["rights"]
        if meta["meta"].get("license"):
            etree.SubElement(m, etree.QName(NS_FCS, "license")).text = meta["meta"]["license"]

    return etree.tostring(root, xml_declaration=True, encoding="UTF-8")


def fcs_searchretrieve_xml(*, records, total, start_record, maximum_records, query_str, version: str = "2.0") -> bytes:
    root = _sru_root("searchRetrieveResponse")
    etree.SubElement(root, etree.QName(NS_SRU, "version")).text = version

    # Echo (gleich wie zuvor)
    echo = etree.SubElement(root, etree.QName(NS_SRU, "echoedSearchRetrieveRequest"))
    etree.SubElement(echo, etree.QName(NS_SRU, "version")).text = version
    etree.SubElement(echo, etree.QName(NS_SRU, "query")).text   = query_str or ""
    etree.SubElement(echo, etree.QName(NS_SRU, "startRecord")).text    = str(start_record)
    etree.SubElement(echo, etree.QName(NS_SRU, "maximumRecords")).text = str(maximum_records)

    # Paging + total
    if maximum_records and start_record + maximum_records <= total:
        etree.SubElement(root, etree.QName(NS_SRU, "nextRecordPosition")).text = str(start_record + maximum_records)
    etree.SubElement(root, etree.QName(NS_SRU, "numberOfRecords")).text = str(total)

    # Records
    recs = etree.SubElement(root, etree.QName(NS_SRU, "records"))
    for i, r in enumerate(records, start=start_record):
        rec = etree.SubElement(recs, etree.QName(NS_SRU, "record"))
        etree.SubElement(rec, etree.QName(NS_SRU, "recordSchema")).text  = NS_FCS
        etree.SubElement(rec, etree.QName(NS_SRU, "recordPacking")).text = "XML"
        rdata = etree.SubElement(rec, etree.QName(NS_SRU, "recordData"))

        res = etree.SubElement(rdata, etree.QName(NS_FCS, "Resource"))
        rh  = etree.SubElement(res, etree.QName(NS_FCS, "ResourceHeader"))
        etree.SubElement(rh, etree.QName(NS_FCS, "title")).text      = r.get("title","")
        etree.SubElement(rh, etree.QName(NS_FCS, "identifier")).text = r.get("id","")

        exts = etree.SubElement(rh, etree.QName(NS_FCS, "extents"))
        for (typ, val) in [
            ("magazine", r.get("magazine","")),
            ("issue",    r.get("issue_label","")),
            ("date",     r.get("issue_date_iso","")),
        ]:
            if val:
                x = etree.SubElement(exts, etree.QName(NS_FCS, "extent"))
                x.set("type", typ)
                x.text = val

        # DataView (KWIC)
        dv = etree.SubElement(res, etree.QName(NS_FCS, "DataView"))
        dv.set("type", "hits:kwic-1.0")
        for l,m,ri in (r.get("kwic_list") or []):
            k = etree.SubElement(dv, etree.QName(NS_HITS, "kwic"))
            etree.SubElement(k, etree.QName(NS_HITS, "leftContext")).text  = l or ""
            etree.SubElement(k, etree.QName(NS_HITS, "match")).text        = m or ""
            etree.SubElement(k, etree.QName(NS_HITS, "rightContext")).text = ri or ""

        etree.SubElement(rec, etree.QName(NS_SRU, "recordPosition")).text = str(i)

    return etree.tostring(root, xml_declaration=True, encoding="UTF-8")


def sru_diagnostic_xml(*, code: str, message: str = "", details: str = "", version: str = "2.0") -> bytes:
    root = _sru_root("searchRetrieveResponse")
    etree.SubElement(root, etree.QName(NS_SRU, "version")).text = version
    etree.SubElement(root, etree.QName(NS_SRU, "numberOfRecords")).text = "0"

    diags = etree.SubElement(root, etree.QName(NS_SRU, "diagnostics"))
    d = etree.SubElement(diags, etree.QName(NS_SRU, "diagnostic"))
    etree.SubElement(d, etree.QName(NS_SRU, "uri")).text = f"info:srw/diagnostic/1/{code}"
    if message:
        etree.SubElement(d, etree.QName(NS_SRU, "message")).text = message
    if details:
        etree.SubElement(d, etree.QName(NS_SRU, "details")).text = details

    return etree.tostring(root, xml_declaration=True, encoding="UTF-8")