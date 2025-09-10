import yfinance as yf
import pika
import json
import time
import sys
import random
import os

RABBITMQ_HOST = os.environ.get('RABBITMQ_HOST', 'localhost')
EXCHANGE_NAME = 'stock_exchange'

def create_connection():
    """RabbitMQ에 대한 연결을 생성"""
    return pika.BlockingConnection(pika.ConnectionParameters(host=RABBITMQ_HOST))

def publish_message(channel, message):
    """메시지를 RabbitMQ exchange에 발행"""
    channel.basic_publish(
        exchange=EXCHANGE_NAME,
        routing_key='', # Fanout exchange는 routing_key가 필요 없습니다.
        body=json.dumps(message),
        properties=pika.BasicProperties(
            content_type='application/json',
            delivery_mode=pika.spec.PERSISTENT_DELIVERY_MODE,  # 메시지를 디스크에 저장하여 RabbitMQ가 재시작되어도 유지되게 합니다.
        )
    )
    print(f" [x] Sent: {message['ticker']} - {message['price']:.2f}")

def main():
    tickers = ["AAPL", "MSFT", "GOOGL", "NVDA", "005930.KS"]

    # 연결 실패 시 5초마다 재시도
    while True:
        try:
            connection = create_connection()
            channel = connection.channel()
            # Fanout 타입의 exchange를 선언 (연결된 모든 큐에 메시지 브로드캐스트)
            # durable=True: RabbitMQ 서버가 재시작되어도 exchange가 사라지지 않도록
            channel.exchange_declare(exchange=EXCHANGE_NAME, exchange_type='fanout', durable=True)
            print(f" [*] RabbitMQ connected to '{RABBITMQ_HOST}'. Starting to send real-time stock data. To exit press CTRL+C")

            while True:
                for ticker in tickers:
                    try:
                        stock = yf.Ticker(ticker)
                        data = stock.history(period='1d', interval='1m')

                        if not data.empty:
                            latest_price = data['Close'].iloc[-1]
                            simulated_price = latest_price * (1 + random.uniform(-0.0005, 0.0005))

                            message = { 'ticker': ticker, 'price': simulated_price, 'timestamp': time.time() }
                            publish_message(channel, message)
                    except Exception as e:
                        print(f"Error fetching data for {ticker}: {e}", file=sys.stderr)

                time.sleep(5) # 5초마다 모든 종목을 한번씩 업데이트

        except pika.exceptions.AMQPConnectionError as e:
            print(f"Connection to RabbitMQ at '{RABBITMQ_HOST}' failed: {e}. Retrying in 5 seconds...")
            time.sleep(5)
        except KeyboardInterrupt:
            print(" [x] Stopped sending messages.")
            if 'connection' in locals() and connection.is_open:
                connection.close()
            break
        except Exception as e:
            print(f"An unexpected error occurred: {e}", file=sys.stderr)
            if 'connection' in locals() and connection.is_open:
                connection.close()
            break

if __name__ == '__main__':
    main()


