package common

import (
    "testing"
)

func TestEncodeDecodeStuff(t *testing.T) {
    salt := "01234567890123456789012345678901"
    payload := "here is your payload"
    
    token, err := Encode(payload, salt)
    if err != nil {
        t.Errorf("Could not encode stuff: %s\n", err)
        return
    }

    decodedPayload, err := Decode(token, salt)
    if err != nil {
        t.Errorf("Could not decode stuff: %s\n", err)
        return
    }
    if decodedPayload != payload {
        t.Errorf("Decoded payload is wrong: '%s' != '%s'\n", payload, decodedPayload)
        return
    }
}

func TestInvalidEncodings(t *testing.T) {
    salt := "salt that is not 32 bytes long"
    payload := "random payload"
    tokenSomehow, err := Encode(payload, salt)
    if err == nil {
        t.Errorf("Somehow encoded token with invalid salt: %s", tokenSomehow)
    }
}

