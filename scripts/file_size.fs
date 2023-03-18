32 constant key_length
4 constant eot
variable rec_key
variable reached?

: read_key
	0
	begin
		dup
		key dup rot cells rec_key + !
		swap 1 + dup rot
	58 = swap key_length >= or until
	drop
;

: debug_key
	key_length 0 do
		rec_key i cells + @ emit
	loop
;

: is_rec_key_file_size?
	0 cells rec_key + @ 102 =
	1 cells rec_key + @ 105 =
	2 cells rec_key + @ 108 =
	3 cells rec_key + @ 101 =
	4 cells rec_key + @ 95 =
	5 cells rec_key + @ 115 =
	6 cells rec_key + @ 105 =
	7 cells rec_key + @ 122 =
	8 cells rec_key + @ 101 =
	and and and and and and and and
;

: is_rec_key_contents?
	0 cells rec_key + @ 99 =
	1 cells rec_key + @ 111 =
	2 cells rec_key + @ 110 =
	3 cells rec_key + @ 116 =
	4 cells rec_key + @ 101 =
	5 cells rec_key + @ 115 =
	6 cells rec_key + @ 110 =
	7 cells rec_key + @ 116 =
	8 cells rec_key + @ 115 =
	and and and and and and and and
;

: skip_line begin key 13 = until ;

: write_key
	key_length 0 do
		rec_key i cells + @ emit
	loop
;

: write_value
	begin
		key dup
		emit
	13 = until
;

: end_reached?
	false reached? !
	key_length 0 do
		rec_key i cells + @
		eot =
		reached? @ or
		reached? !
	loop
	reached? @
;

( ######## )
( # MAIN # )
( ######## )
: setup
	rec_key key_length cells allot
	key_length 0 do
		0 rec_key i cells + !
	loop

	false reached? !
;

: draw
	read_key
	debug_key
	is_rec_key_file_size? if
		skip_line
	else is_rec_key_contents? if
		( TODO write rec key )
		( TODO count contents length )
		( write_key )
		write_value
	else
		( TODO write key )
		( TODO write value )
		( write_key )
		write_value
	then then
;

: main
	setup
	begin draw
	end_reached? until
;

main

