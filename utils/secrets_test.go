package utils

import (
	"fmt"
	"testing"
)

func compareMaps(a, b map[string]any) bool {
	x := fmt.Sprintf("%#v", a)
	y := fmt.Sprintf("%#v", b)
	return x == y
}

func TestEncodingAndDecodingSecrets(t *testing.T) {
	original := map[string]any {
		"name": "Marceline",
		"age": 1000,
		"interests": []any {
			"bass",
		},
	}
	hiddenSecret, err := EncodeSecret(original)
	if err != nil {
		t.Fatalf("Failed to encode secret: %s", err)
		return
	}

	decoded, err := DecodeSecret(hiddenSecret)
	if err != nil {
		t.Fatalf("Failed to decode secret: %s", err)
		return
	}

	if !compareMaps(original, decoded) {
		t.Fatalf("Secrets were not properly encoded\nexpected: %#v\nobtained: %#v\n", original, decoded)
		return
	}
}

func TestHidingSecrets(t *testing.T) {
	secret := "random string"
	hiddenSecret := HideSecret(secret)
	if secret == hiddenSecret {
		t.Fatalf("Failed to hide secret")
		return
	}
}

