function main(inlet)
 local params = parse_url_params(inlet)
 local filename = params["filename"]
 return download_user_file(filename)
end
