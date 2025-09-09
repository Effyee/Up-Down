import yfinance as yf
import pandas as pd
import json
import sys

def get_stock_prices(tickers):
    """
    주어진 티커 목록에 대한 최신 주가 정보를 가져와 JSON 형태로 반환합니다.
    """
    # yfinance는 여러 티커를 공백으로 구분하여 한 번에 조회할 수 있습니다.
    ticker_string = " ".join(tickers)

    # 지난 5일간의 데이터를 가져와서, 가장 최신 데이터를 찾습니다.
    data = yf.download(tickers=ticker_string, period="5d", group_by='ticker')

    results = []

    for ticker in tickers:
        try:
            # 해당 티커의 데이터만 선택합니다.
            stock_data = data[ticker]
            # 유효한 데이터(NaN이 아닌) 중 가장 마지막 행을 선택합니다.
            latest_data = stock_data.dropna().iloc[-1]

            price_info = {
                "ticker": ticker,
                "closePrice": latest_data["Close"],
                "openPrice": latest_data["Open"],
                "highPrice": latest_data["High"],
                "lowPrice": latest_data["Low"],
                "volume": latest_data["Volume"]
            }
            results.append(price_info)
        except Exception as e:
            # 특정 티커 조회에 실패할 경우 에러를 출력하고 계속 진행합니다.
            print(f"Error fetching data for {ticker}: {e}", file=sys.stderr)

    # 최종 결과를 JSON 문자열로 변환하여 출력합니다.
    return json.dumps(results, indent=4)

if __name__ == "__main__":
    # 커맨드 라인 인자로부터 티커 목록을 받습니다.
    # 예: python get_stock_data.py 005930.KS AAPL MSFT
    tickers_from_args = sys.argv[1:]
    if not tickers_from_args:
        print("Usage: python get_stock_data.py <ticker1> <ticker2> ...", file=sys.stderr)
        sys.exit(1)

    prices_json = get_stock_prices(tickers_from_args)
    # 최종 JSON 결과를 표준 출력(stdout)으로 내보냅니다. Spring Boot가 이 결과를 읽게 됩니다.
    print(prices_json)

