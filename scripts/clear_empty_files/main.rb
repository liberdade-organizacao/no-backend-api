#!ruby

def main argv
  input_dir, output_dir = argv
  Dir["#{input_dir}/*"].each do |input_file|
    contents = File.read(input_file).strip
    if contents.length > 0
      output_file = input_file.gsub input_dir, output_dir
      File.open(output_file, "w") do |output|
        output.write contents
      end
    end
  end
end

if __FILE__ == $0
  main ARGV
end
