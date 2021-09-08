# WebVTT to SRT
**Simple WebVTT to SRT subtitle converter.**

Made because no converter I found would actually preserve
basic formatting like italics. This one does.

Parameters:
- `-i`/`--input` Specifies a filename or a glob pattern
  (? is any character, * is any string)  
  Notice that every file that fits a pattern may be attempted
  to be converted, not only `.vtt`.
- `-s`/`--starting-path` Specifies the starting path to search for
  files (default `./`)
- `-D`/`--delete-original` Deletes the original files after conversion
- `-r`/`--recursive` Allows searching for glob pattern files in subdirectories
  (unoptimized)

For example:  
`java -jar webvtttosrt.jar -- -iDr *.vtt`  
Finds and converts all `.vtt` files in the current directory
to `.srt` and then removes the original files.