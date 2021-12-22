set -e

gsutil -m cp -r -z "html" parcel-redirect.html gs://datax-research-public/parcel-redirect/index.html
gsutil setmeta \
  -h "content-type: text/html;charset=utf-8;" \
  -h "cache-control: no-cache, no-store, must-revalidate" \
  gs://datax-research-public/parcel-redirect/index.html
