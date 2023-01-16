function main(inlet)
  local params = parse_url_params(inlet)
  local filename = params["filename"]
  local contents = params["contents"]
  upload_file(filename, contents)
  return "ok"
end
