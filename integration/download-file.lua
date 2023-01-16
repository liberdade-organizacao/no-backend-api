function main(inlet)
 local params = parse_url_params(inlet)
 local filename = params["filename"]
 return download_file(filename)
end
