# PDND Interoperability - Party Registry Proxy Micro Service

---

### Setup reload endpoint

By default, at startup, this component loads a Lucene dataset with all the available institutions and categories.  
For testing purposes, you could enable a hidden endpoint to manually reload the dataset.  

To enable it, you should launch the component with the following Java option:  

`-Dconfig.reload_data_endpoint=true`  

This enables the `GET reload` endpoint that eagerly refresh the Lucene data.