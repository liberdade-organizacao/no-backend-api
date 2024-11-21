package utils

import (
	"testing"
)

func TestCanFormatSqlQueries(t *testing.T) {
	expected := "SELECT * FROM table LIMIT 10;"
	template := "SELECT * FROM %{table} LIMIT %{count};"
	params := map[string]any {
		"table": "table",
		"count": 10,
		"extra": "extra field",
	}
	obtained, err := Format(template, params)
	if err != nil {
		t.Fatalf("failed to format: %s", err)
		return
	}
	if expected != obtained {
		t.Fatalf("template wrongly filled:\nexpected: %s\nobtained: %s", expected, obtained)
		return
	}
}

