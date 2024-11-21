package utils

import (
	"fmt"
	"strings"
)

func Format(template string, params map[string]any) (string, error) {
	outlet := template

	for key, value := range params {
		outlet = strings.ReplaceAll(
			outlet,
			fmt.Sprintf("%%{%s}", key),
			fmt.Sprintf("%v", value),
		)
	}

	return outlet, nil
}

