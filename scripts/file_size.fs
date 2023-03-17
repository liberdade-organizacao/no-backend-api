32 constant key_length
variable rec_key

: read_key
	0
	begin
		dup
		key dup rot rec_key cells + !
		swap 1 + dup rot
	58 = swap key_length >= or until
	drop
;

: debug_key
	0
	begin
		dup
		rec_key cells @ emit
		1 + dup
	32 >= until
	drop
;

( ######## )
( # MAIN # )
( ######## )
( initializing variables )
rec_key key_length cells allot
read_key
debug_key

