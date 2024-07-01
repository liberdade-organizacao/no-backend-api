#include <stdio.h>
#include <stdlib.h>
#include <dirent.h>
#include <string.h>

char** list_files_in_dir(char* dirname) {
	DIR *d = NULL;
	struct dirent *dir = NULL;
	char** outlet = NULL;
	char* filename = NULL;
	char* file_identifier = NULL;
	int slen = 0;
	int i = 0;

	if (dirname == NULL) {
		return NULL;
	}

	d = opendir(dirname);
	if (d) {
		/* allocating initial memory */
		i = 0;
		outlet = (char**) malloc(sizeof(char**));

		/* insert every filename into */
		while ((dir = readdir(d)) != NULL) {
			filename = dir->d_name;
			if (filename[0] != '.') {
				i++;
				outlet = (char**) realloc(
					outlet,
					(i + 1) * sizeof(char**)
				);
				outlet[i - 1] = filename;
			}
		}

		/* clean up */
		closedir(d);
		outlet[i] = NULL;
	}

	return outlet;
}

void transfer_fragments(char* input_dir, char* input_file, char* output_dir) {
	int i = 0, j = 0;
	int len = 0;
	int indlen = strlen(input_dir);
	int inflen = strlen(input_file);
	int oudlen = strlen(output_dir);
	int offset = 0;
	char* input_path = NULL;
	int fragment_count = 0;
	char* output_template = NULL;
	char* output_path = NULL;
	char c;
	FILE* inlet;
	FILE* outlet;
	
	/* ALLOCATING MEMORY */
	/* building filename */
	len = 2 + indlen + inflen; /* includes '/' and '\0' */
	input_path = (char*) malloc(len * sizeof(char));
	sprintf(input_path, "%s/%s", input_dir, input_file);

	/* get output file template */
	len = 8 + oudlen + inflen; /* includes "-%03d", '/' , and '\0' */ 
	output_template = (char*) malloc(len * sizeof(char));
	output_path = (char*) malloc(len * sizeof(char));

	j = 0;
	for (i = 0; i < oudlen; i++) {
		output_template[j++] = output_dir[i];
	}
	output_template[j++] = '/';
	for (i = 0; i < inflen; i++) {
		if (input_file[i] == '-') {
			output_template[j++] = '-';
			output_template[j++] = '%';
			output_template[j++] = '0';
			output_template[j++] = '3';
			output_template[j++] = 'd';
		}
		output_template[j++] = input_file[i];
	}

	/* TRANSFERING MEMORY */
	/* reading file to get fragments */
	inlet = fopen(input_path, "r");

	if (inlet == NULL) {
		return;
	}

	j = 0;
	sprintf(output_path, output_template, j);
	outlet = fopen(output_path, "w");
	c = fgetc(inlet);
	while (!feof(inlet)) {
		fputc(c, outlet);
		if (c == ';') {
			fflush(outlet);
			fclose(outlet);
			sprintf(output_path, output_template, ++j);
			outlet = fopen(output_path, "w");
		}
		c = fgetc(inlet);
	}

	fclose(inlet);
	fclose(outlet);

	return;
}

void evaluate_dir(char* input_dir, char** input_files, char* output_dir) {
	int i = 0;

	for (i = 0; input_files[i] != NULL; i++) {
		transfer_fragments(input_dir, input_files[i], output_dir);
	}

	return;
}

int main(int argc, char* argv[]) {
	int i = 0;
	char* input_dirname = NULL;
	char* output_dirname = NULL;
	char** input_filenames = NULL;

	/* read */
	if (argc != 3) {
		return 1;
	}
	input_dirname = argv[1];
	output_dirname = argv[2];

	/* evaluate */
	input_filenames = list_files_in_dir(input_dirname);

	/* print */
	evaluate_dir(input_dirname, input_filenames, output_dirname);

	return 0;
}

