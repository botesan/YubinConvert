use std::io::Read;
use std::num::NonZeroU64;
use std::ptr;
use std::slice;
use zenzop::{Format, Options};

/// Compress input using zenzop.
///
/// Returns:
///   >= 0 : compressed size
///   -1   : invalid argument
///   -2   : output buffer too small
///   -3   : compression error
#[unsafe(no_mangle)]
pub unsafe extern "C" fn zenzop_compress(
    input: *const u8,
    input_len: usize,
    output: *mut u8,
    output_capacity: usize,
    iterations: u64,
    maximum_block_splits: u16,
    enhanced: bool,
) -> i64 {
    if input.is_null() || output.is_null() {
        return -1;
    }
    if iterations == 0 {
        return -1;
    }

    let mut options = Options::default();
    options.iteration_count = NonZeroU64::new(iterations).unwrap();
    options.maximum_block_splits = maximum_block_splits;
    options.enhanced = enhanced;

    let input_slice = unsafe { slice::from_raw_parts(input, input_len) };
    let input_wrapper = ReadWrapper::new(input_slice);
    let mut compressed = Vec::new();
    let result = zenzop::compress(options, Format::Gzip, input_wrapper, &mut compressed);
    if result.is_err() {
        return -3;
    }
    if compressed.len() > output_capacity {
        return -2;
    }

    unsafe {
        ptr::copy_nonoverlapping(compressed.as_ptr(), output, compressed.len());
    }

    compressed.len() as i64
}

// ラップしてスライスの Read の fn read_buf をデフォルトの動作にする
// スライスの Read の fn read_buf の実装だと動作が異なるみたい
// std::io::copy で一気に Encoder へ write するのが良くないっぽい？

struct ReadWrapper<R> {
    inner: R,
}

impl<R> ReadWrapper<R> {
    const fn new(inner: R) -> Self {
        Self { inner }
    }
}

impl<R: Read> Read for ReadWrapper<R> {
    fn read(&mut self, buf: &mut [u8]) -> std::io::Result<usize> {
        self.inner.read(buf)
    }
}

#[cfg(test)]
mod tests {
    use crate::ReadWrapper;
    use zenzop::{Format, Options};

    fn compress(count: u32) {
        let input: Vec<u8> = (0..count).map(|x| x as u8).collect();
        //
        let input_slice = input.as_slice();
        let mut compressed1 = Vec::new();
        let result = zenzop::compress(
            Options::default(),
            Format::Gzip,
            input_slice,
            &mut compressed1,
        );
        assert_eq!(result.is_ok(), true);
        //
        let inputs_wrap = ReadWrapper::new(input_slice);
        let mut compressed2 = Vec::new();
        let result = zenzop::compress(
            Options::default(),
            Format::Gzip,
            inputs_wrap,
            &mut compressed2,
        );
        assert_eq!(result.is_ok(), true);
        //
        assert_eq!(
            compressed1,
            compressed2,
            "count={}, compressed1.len()={}, compressed2.len()={}",
            count,
            compressed1.len(),
            compressed2.len()
        );
    }
    #[test]
    fn test_ng() {
        compress((1024 * 31 / 32 - 15) * 1024); //977
    }
    #[test]
    fn test_ok() {
        compress((1024 * 15 / 16 + 16) * 1024); //976
    }
}
